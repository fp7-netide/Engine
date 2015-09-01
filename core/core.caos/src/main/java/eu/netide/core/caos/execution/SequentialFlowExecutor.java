package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ExecutionResult;
import eu.netide.core.caos.resolution.ConflictResolvers;
import eu.netide.core.caos.resolution.IConflictResolver;
import eu.netide.core.caos.resolution.ResolutionResult;
import eu.netide.lib.netip.Message;

import java.util.Arrays;

/**
 * Created by timvi on 31.08.2015.
 */
public class SequentialFlowExecutor implements IFlowExecutor {
    @Override
    public ExecutionFlowStatus executeFlow(ExecutionFlowStatus status, Iterable<ExecutionFlowNode> nodes, IBackendManager backendManager) {
        for (ExecutionFlowNode efn : nodes) {
            ExecutionResult result = FlowNodeExecutors.getExecutor(efn).execute(efn, status, backendManager); // request results
            // merge results
            IConflictResolver resolver = ConflictResolvers.getMatchingResolver(result.getMessagesToSend().toArray(Message[]::new));
            ResolutionResult rr = resolver.resolve(status.getResultMessages().stream().toArray(Message[]::new), result.getMessagesToSend().toArray(Message[]::new), true);
            status.setResultMessages(Arrays.asList(rr.getResultingMessagesToSend()));
        }
        return status;
    }
}
