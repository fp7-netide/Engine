package eu.netide.core.routing;

import eu.netide.core.api.IAsyncRequestManager;
import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IBackendMessageListener;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.api.MessageHandlingResult;
import eu.netide.lib.netip.Message;
import org.apache.felix.scr.annotations.Component;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import java.util.LinkedList;

/**
 * Created by arne on 28.06.16.
 */
@Component(immediate=true)
@Service
public class OpenFlowRouting implements IAsyncRequestManager, IShimMessageListener, IBackendMessageListener {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IBackendManager backendManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IShimManager shimManager;

    @Override
    public void sendAsyncRequest(Message m, int moduleId) {

    }

    @Override
    public MessageHandlingResult OnShimMessage(Message message, String originId) {

        return MessageHandlingResult.RESULT_PASS;
    }

    @Override
    public void OnOutgoingShimMessage(Message message) {

    }

    @Override
    public MessageHandlingResult OnBackendMessage(Message message, String originId) {

        return MessageHandlingResult.RESULT_PASS;
    }

    @Override
    public void OnBackendRemoved(String backEndName, LinkedList<Integer> removedModules) {

    }

    @Override
    public void OnOutgoingBackendMessage(Message message, String backendId) {

    }
}
