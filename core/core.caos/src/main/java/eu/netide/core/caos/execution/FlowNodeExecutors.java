package eu.netide.core.caos.execution;

import eu.netide.core.caos.composition.ExecutionFlowNode;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by timvi on 31.08.2015.
 */
public class FlowNodeExecutors {

    private static final IFlowNodeExecutor[] ALL_FLOW_NODE_EXECUTORS = new IFlowNodeExecutor[]{
            new ParallelCallNodeExecutor(), new ModuleCallNodeExecutor(), new BranchNodeExecutor()
    };

    public static IFlowNodeExecutor getExecutor(ExecutionFlowNode node) {
        Optional<IFlowNodeExecutor> e = Arrays.stream(ALL_FLOW_NODE_EXECUTORS).filter(ex -> ex.canExecute(node)).findFirst();
        if (!e.isPresent())
            throw new UnsupportedOperationException("No executor known for node type '" + node.getClass().getName() + "'");
        return e.get();
    }
}
