package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.RequestResult;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ExecutionResult;
import eu.netide.core.caos.composition.ModuleCall;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageHeader;
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
    public ExecutionResult execute(ExecutionFlowNode node, ExecutionFlowStatus status, IBackendManager backendManager) {
        if (!canExecute(node)) throw new UnsupportedOperationException("Cannot execute this type of node!");

        ModuleCall mc = (ModuleCall) node;

        // check for conditions
        if ((mc.getCallCondition() != null && !ConditionEvaluator.evaluate(mc.getCallCondition(), status)
                || (mc.getModule().getCallCondition() != null && !ConditionEvaluator.evaluate(mc.getModule().getCallCondition(), status)))) {
            return ExecutionResult.SKIPPED;
        }

        MessageHeader header = new MessageHeader();
        header.setTransactionId(42); // TODO how to determine transaction IDs
        header.setDatapathId(1); // TODO how to determine datapath IDs
        header.setModuleId(mc.hashCode()); // TODO get module id
        
        logger.debug("Sending Request...");
        Message message = new Message(header, status.getCurrentMessage().getPayload());
        RequestResult result = backendManager.sendRequest(message);
        logger.debug("Request returned.");
        ExecutionResult exresult = new ExecutionResult();
        for (Message m : result.getResultMessages())
            exresult.addMessageToSend(m);
        return exresult;
    }
}
