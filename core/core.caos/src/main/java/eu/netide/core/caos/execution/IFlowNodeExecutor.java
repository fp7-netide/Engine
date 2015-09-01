package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ExecutionResult;

/**
 * Created by timvi on 31.08.2015.
 */
public interface IFlowNodeExecutor {
    boolean canExecute(ExecutionFlowNode node);

    ExecutionResult execute(ExecutionFlowNode node, ExecutionFlowStatus status, IBackendManager backendManager);
}
