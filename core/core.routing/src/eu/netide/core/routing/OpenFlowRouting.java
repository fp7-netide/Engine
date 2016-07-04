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
import org.apache.commons.lang3.tuple.Pair;
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

/**
 * Created by arne on 28.06.16.
 */
@Component(immediate = true)
@Service
public class OpenFlowRouting implements IOFRoutingManager, IShimMessageListener, IBackendMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(OpenFlowRouting.class);


    private static OFType OFSyncRequests[] = {OFType.ECHO_REQUEST, OFType.FEATURES_REQUEST,
            OFType.GET_ASYNC_REQUEST, OFType.GET_CONFIG_REQUEST, OFType.QUEUE_GET_CONFIG_REQUEST,
            OFType.STATS_REQUEST
    };

    private static OFType OFSyncResults[] = {OFType.ECHO_REPLY, OFType.FEATURES_REPLY,
            OFType.GET_ASYNC_REPLY, OFType.GET_CONFIG_REPLY, OFType.QUEUE_GET_CONFIG_REPLY,
            OFType.STATS_REPLY
    };


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IBackendManager backendManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IShimManager shimManager;

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

    @Override
    public Collection<? extends OFRoutingRequest> getRoutingRequestStatus() {
        return backendXidToRequest.values();
    }

    private HashMap<Long, Request> shimXidToRequest = new LinkedHashMap<>();
    private HashMap<Pair<Long, String>, Request> backendXidToRequest = new LinkedHashMap<>();


    @Override
    public void sendRequest(Message m, int moduleId) {
        // TODO: Implement
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
                    handleResponse(openFlowMessage, message);
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
        Pair<Long, String> key = Pair.of(xid, originId);

        if (!backendXidToRequest.containsKey(key)) {
            Request req = new Request(originId, xid, type);
            backendXidToRequest.put(key, req);
            shimXidToRequest.put(req.shimXid, req);
        }

        return backendXidToRequest.get(key);

    }

    private void handleResponse(OFMessage openFlowMessage, Message message) {
        long xid = openFlowMessage.getXid();
        Request req = shimXidToRequest.get(xid);
        if (req == null) {
            logger.info("Could not find response in Request table, elaying to ALL backends");
            backendManager.sendMessageAllBackends(message);
        } else {
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
