package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;

/**
 * Created by timvi on 31.08.2015.
 */
public interface IFlowExecutor {
    ExecutionFlowStatus executeFlow(ExecutionFlowStatus status, Iterable<ExecutionFlowNode> nodes, IBackendManager backendManager);
}
