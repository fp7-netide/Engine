package eu.netide.core.routing;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IBackendMessageListener;
import eu.netide.core.api.IOFStatRequestManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.api.MessageHandlingResult;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.OpenFlowMessage;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;

import java.util.LinkedList;

/**
 * Created by arne on 28.06.16.
 */
@Component(immediate = true)
@Service
public class OpenFlowRouting implements IOFStatRequestManager, IShimMessageListener, IBackendMessageListener {
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

    @Override
    public void sendAsyncRequest(Message m, int moduleId) {

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
                    handleAsyncResponse(openFlowMessage, originId);
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
                if (openFlowMessage.getType()==type) {
                    handleAsyncRequest(openFlowMessage, originId);
                    return MessageHandlingResult.RESULT_PROCESSED;
                }

        }
        return MessageHandlingResult.RESULT_PASS;
    }


    private void handleAsyncRequest(OFMessage openFlowMessage, String originId) {

    }

    private void handleAsyncResponse(OFMessage openFlowMessage, String originId) {

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
