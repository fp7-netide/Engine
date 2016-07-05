package eu.netide.core.routing;

import eu.netide.core.api.Constants;
import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IBackendMessageListener;
import eu.netide.core.api.IOFRoutingManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.api.MessageHandlingResult;
import eu.netide.core.api.OFRoutingRequest;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.OpenFlowMessage;
import org.javatuples.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by arne on 28.06.16.
 */
@Component(immediate = true)
@Service
public class OpenFlowRouting implements IOFRoutingManager, IShimMessageListener, IBackendMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(OpenFlowRouting.class);

    private static int REQUEST_TIMEOUT = 120;
    private static final int CLEANUP_PERIOD = 60 * 1000;


    private static OFType OFSyncRequests[] = {OFType.ECHO_REQUEST, OFType.FEATURES_REQUEST,
            OFType.GET_ASYNC_REQUEST, OFType.GET_CONFIG_REQUEST, OFType.QUEUE_GET_CONFIG_REQUEST,
            OFType.STATS_REQUEST
    };

    private static OFType OFSyncResults[] = {OFType.ECHO_REPLY, OFType.FEATURES_REPLY,
            OFType.GET_ASYNC_REPLY, OFType.GET_CONFIG_REPLY, OFType.QUEUE_GET_CONFIG_REPLY,
            OFType.STATS_REPLY
    };
    private final Timer cleanupTimer;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IBackendManager backendManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IShimManager shimManager;


    void setShim(IShimManager shim) {
        shimManager =shim;
    }

    void setBackend(IBackendManager backend)
    {
        backendManager = backend;
    }


    public OpenFlowRouting ()
    {
        cleanupTimer = new Timer("Routing request cleanup");
        TimerTask cleanUpTask = new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Request r : backendXidToRequest.values()) {
                    if (r.getLastTimeActive() < now + REQUEST_TIMEOUT * 1000) {
                        backendXidToRequest.remove(Pair.with(r.getBackendXid(), r.getBackendID()));
                        shimXidToRequest.remove(r.getNewShimXid());
                    }
                }
            }
        };
        cleanupTimer.schedule(cleanUpTask, CLEANUP_PERIOD, CLEANUP_PERIOD);
    }

    static class Request implements OFRoutingRequest {
        @Override
        public long getBackendXid() {
            return backendXid;
        }

        @Override
        public long getShimXid() {
            return shimXid;
        }

        @Override
        public String getBackendID() {
            return backendID;
        }


        @Override
        public String getReqTypeString() {
            return reqType.toString();
        }

        @Override
        public long getLastTimeActive() {
            return lastTimeActive;
        }

        @Override
        public int getResponses() {
            return responses;
        }

        final long backendXid;
        final long shimXid;
        final String backendID;
        final OFType reqType;

        long lastTimeActive = 0;
        int responses = 0;
        static long lastUsedShimXid = Constants.FIRST_SHIM_XID;

        Request(String originId, long backendXid, OFType type) {
            this.backendXid = backendXid;
            backendID = originId;
            lastTimeActive = System.currentTimeMillis();
            shimXid = getNewShimXid();
            responses = 0;
            reqType = type;
        }

        private long getNewShimXid() {
            if (lastUsedShimXid >= Constants.LAST_SHIM_XID) {
                lastUsedShimXid = Constants.FIRST_SHIM_XID;
            }
            return ++lastUsedShimXid;
        }

        void countResponse() {
            responses++;
            lastTimeActive = System.currentTimeMillis();
        }
    }

    private ConcurrentHashMap<Long, Request> shimXidToRequest = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Pair<Long, String>, Request> backendXidToRequest = new ConcurrentHashMap<>();

    @Override
    public Collection<? extends OFRoutingRequest> getRoutingRequestStatus() {
        return backendXidToRequest.values();
    }

    @Override
    public void sendRequest(OpenFlowMessage m, String moduleId) {
        handleRequest(m.getOfMessage(), m, moduleId);
    }

    @Override
    public MessageHandlingResult OnShimMessage(Message message, String originId) {
        if (message.getHeader().getMessageType() != MessageType.OPENFLOW) {
            return MessageHandlingResult.RESULT_PASS;
        }
        if (message instanceof OpenFlowMessage) {
            OFMessage openFlowMessage = ((OpenFlowMessage) message).getOfMessage();


            for (OFType type : OFSyncResults) {
                if (openFlowMessage.getType() == type) {
                    handleResponse(openFlowMessage, (OpenFlowMessage) message);
                    return MessageHandlingResult.RESULT_PROCESSED;
                }

            }
        }
        return MessageHandlingResult.RESULT_PASS;
    }


    @Override
    public MessageHandlingResult OnBackendMessage(Message message, String
            originId) {

        if (message.getHeader().getMessageType() != MessageType.OPENFLOW) {
            return MessageHandlingResult.RESULT_PASS;
        }

        if (message instanceof OpenFlowMessage) {
            OFMessage openFlowMessage = ((OpenFlowMessage) message).getOfMessage();

            for (OFType type : OFSyncRequests)
                if (openFlowMessage.getType() == type) {
                    handleRequest(openFlowMessage, message, originId);
                    return MessageHandlingResult.RESULT_PROCESSED;
                }

        }
        return MessageHandlingResult.RESULT_PASS;
    }


    private void handleRequest(OFMessage openFlowMessage, Message message, String originId) {
        Request request = getOrAddBackendRequest(openFlowMessage.getXid(), originId, openFlowMessage.getType());
        OFMessage.Builder b = openFlowMessage.createBuilder();
        b.setXid(request.shimXid);

        OpenFlowMessage ofMessage = new OpenFlowMessage();
        ofMessage.setOfMessage(b.build());

        // TODO: Clone msg header first?
        ofMessage.setHeader(message.getHeader());
        ofMessage.getHeader().setTransactionId((int) request.shimXid);

        shimManager.sendMessage(ofMessage);

    }

    private Request getOrAddBackendRequest(long xid, String originId, OFType type) {
        Pair<Long, String> key = Pair.with(xid, originId);

        if (!backendXidToRequest.containsKey(key)) {
            Request req = new Request(originId, xid, type);
            backendXidToRequest.put(key, req);
            shimXidToRequest.put(req.shimXid, req);
        }

        return backendXidToRequest.get(key);

    }

    private void handleResponse(OFMessage openFlowMessage, OpenFlowMessage message) {
        long xid = openFlowMessage.getXid();
        Request req = shimXidToRequest.get(xid);
        if (req == null) {
            logger.info("Could not find response in Request table, elaying to ALL backends: {}", message);
            backendManager.sendMessageAllBackends(message);
        } else {
            OFMessage.Builder b = openFlowMessage.createBuilder();
            b.setXid(req.backendXid);
            message.setOfMessage(b.build());
            req.countResponse();
            message.getHeader().setModuleId(backendManager.getModuleId(req.backendID));
            backendManager.sendMessage(message);
        }
    }


    @Override
    public void OnOutgoingShimMessage(Message message) {

    }

    @Override
    public void OnUnhandeldShimMessage(Message message, String originId) {

    }

    @Override
    public void OnBackendRemoved(String backEndName, LinkedList<Integer> removedModules) {

    }

    @Override
    public void OnOutgoingBackendMessage(Message message, String backendId) {

    }

    @Override
    public void OnUnhandledBackendMessage(Message message, String originId) {

    }
}
