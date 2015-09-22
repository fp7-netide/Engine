package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.RequestResult;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ModuleCall;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageHeader;
import eu.netide.lib.netip.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by timvi on 31.08.2015.
 */
public class ModuleCallNodeExecutor implements IFlowNodeExecutor {

    private final Logger logger = LoggerFactory.getLogger(ModuleCallNodeExecutor.class);

    @Override
    public boolean canExecute(ExecutionFlowNode node) {
        return node instanceof ModuleCall;
    }

    @Override
    public ExecutionFlowStatus execute(ExecutionFlowNode node, ExecutionFlowStatus status, IShimManager shimManager, IBackendManager backendManager) {
        if (!canExecute(node)) throw new UnsupportedOperationException("Cannot execute this type of node!");

        ModuleCall mc = (ModuleCall) node;

        // check for conditions
        if ((mc.getCallCondition() != null && !ConditionEvaluator.evaluate(mc.getCallCondition(), status)
                || (mc.getModule().getCallCondition() != null && !ConditionEvaluator.evaluate(mc.getModule().getCallCondition(), status)))) {
            return status;
        }

        MessageHeader header = new MessageHeader();
        header.setMessageType(MessageType.OPENFLOW);
        header.setTransactionId(status.getCurrentMessage().getHeader().getTransactionId());
        header.setDatapathId(status.getCurrentMessage().getHeader().getDatapathId());
        int moduleId = backendManager.getModuleId(mc.getModule().getId());
        header.setModuleId(moduleId);

        logger.info("Sending Request to module '" + moduleId + "'...");
        Message message = new Message(header, status.getCurrentMessage().getPayload());
        RequestResult result = backendManager.sendRequest(message);
        logger.info("Request returned from module '" + moduleId + "'.");
        return ExecutionUtils.mergeMessagesIntoStatus(status, result.getResultMessages().toArray(Message[]::new));
    }
}
