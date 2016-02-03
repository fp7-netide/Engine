package eu.netide.core.caos.execution;

/**
 * Created by timvi on 31.08.2015.
 */
public class FlowExecutors {

    /**
     * The default flow executor for sequential flows.
     */
    public static final IFlowExecutor SEQUENTIAL = new SequentialFlowExecutor();
}
