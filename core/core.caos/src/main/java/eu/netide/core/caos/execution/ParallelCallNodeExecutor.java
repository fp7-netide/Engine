package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.*;
import eu.netide.core.caos.resolution.ConflictResolvers;
import eu.netide.core.caos.resolution.IConflictResolver;
import eu.netide.core.caos.resolution.PriorityInfo;
import eu.netide.core.caos.resolution.ResolutionResult;
import eu.netide.lib.netip.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Executor for ParallelCall nodes.
 * <p>
 * Created by timvi on 31.08.2015.
 */
public class ParallelCallNodeExecutor implements IFlowNodeExecutor {

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Logger logger = LoggerFactory.getLogger(ParallelCall.class);

    @Override
    public boolean canExecute(ExecutionFlowNode node) {
        return node instanceof ParallelCall;
    }

    @Override
    public ExecutionResult execute(ExecutionFlowNode node, ExecutionFlowStatus status, IBackendManager backendManager) {
        if (!canExecute(node)) throw new UnsupportedOperationException("Cannot execute this type of node!");

        ParallelCall pc = (ParallelCall) node;

        List<Future<ExecutionResult>> futures = new ArrayList<>();
        for (ModuleCall mc : pc.getModuleCalls()) {
            futures.add(pool.submit(() -> FlowNodeExecutors.getExecutor(mc).execute(mc, status, backendManager)));
        }
        Message[] results = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Unable to get result of ModuleCall", e);
                throw new RuntimeException("Unable to get result of ModuleCall", e);
            }
        }).flatMap(ExecutionResult::getMessagesToSend).toArray(Message[]::new);

        IConflictResolver resolver = ConflictResolvers.getMatchingResolver(results);
        ResolutionResult resolutionResult = resolver.resolve(results, pc.getResolutionPolicy(), PriorityInfo.fromModuleCalls(pc.getModuleCalls(), backendManager));
        ExecutionResult executionResult = new ExecutionResult();
        Arrays.stream(resolutionResult.getResultingMessagesToSend()).forEach(executionResult::addMessageToSend);
        return executionResult;
    }
}
