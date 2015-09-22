package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.caos.composition.Branch;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ParallelCall;
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
    public ExecutionFlowStatus execute(ExecutionFlowNode node, ExecutionFlowStatus status, IShimManager shimManager, IBackendManager backendManager) {
        if (!canExecute(node)) throw new UnsupportedOperationException("Cannot execute this type of node!");

        Branch br = (Branch) node;
        boolean ifBranch = ConditionEvaluator.evaluate(br.getCondition(), status);
        logger.info("Taking " + (ifBranch ? "IF" : "ELSE") + " path of branch.");
        return FlowExecutors.SEQUENTIAL.executeFlow(status, ifBranch ? br.getIf().getFlowNodes().stream() : br.getElse().getFlowNodes().stream(), shimManager, backendManager);
    }
}
