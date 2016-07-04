package eu.netide.core.routing;

import eu.netide.core.api.IAsyncRequestManager;
import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IBackendMessageListener;
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
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;

import java.util.LinkedList;

/**
 * Created by arne on 28.06.16.
 */
@Component(immediate = true)
@Service
public class OpenFlowRouting implements IAsyncRequestManager, IShimMessageListener, IBackendMessageListener {
    private static Class OFAsyncRequests[] = {OFStatsRequest.class};
    private static Class OFAsyncResults[] = {OFStatsReply.class};

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

            for (Class respClass : OFAsyncResults) {
                if (respClass.isInstance(openFlowMessage)) {
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

            for (Class reqClass : OFAsyncRequests)
                if (reqClass.isInstance(openFlowMessage)) {
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
    public void OnBackendRemoved(String backEndName, LinkedList<Integer> removedModules) {

    }

    @Override
    public void OnOutgoingBackendMessage(Message message, String backendId) {

    }
}
