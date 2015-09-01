package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor for branch nodes.
 * <p>
 * Created by timvi on 31.08.2015.
 */
public class BranchNodeExecutor implements IFlowNodeExecutor {

    private final Logger logger = LoggerFactory.getLogger(ParallelCall.class);

    @Override
    public boolean canExecute(ExecutionFlowNode node) {
        return node instanceof Branch;
    }

    @Override
    public ExecutionResult execute(ExecutionFlowNode node, ExecutionFlowStatus status, IBackendManager backendManager) {
        if (!canExecute(node)) throw new UnsupportedOperationException("Cannot execute this type of node!");

        Branch br = (Branch) node;
        ExecutionResult executionResult = new ExecutionResult();
        ExecutionFlowStatus s = FlowExecutors.SEQUENTIAL.executeFlow(status, ConditionEvaluator.evaluate(br.getCondition(), status) ? br.getIf().getFlowNodes() : br.getElse().getFlowNodes(), backendManager);
        s.getResultMessages().forEach(executionResult::addMessageToSend);
        return executionResult;
    }
}
