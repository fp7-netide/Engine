package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;

import java.util.stream.Stream;

/**
 * Created by timvi on 31.08.2015.
 */
public interface IFlowExecutor {
    ExecutionFlowStatus executeFlow(ExecutionFlowStatus status, Stream<ExecutionFlowNode> nodes, IBackendManager backendManager);
}
