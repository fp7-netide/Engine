package eu.netide.core.api.composition;

import eu.netide.core.api.NetworkApplicationInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timvi on 25.06.2015.
 */
public class CompositionSpecification {
    private List<NetworkApplicationInfo> _subApplications;
    private List<ExecutionFlowNode> _executionFlow;

    public CompositionSpecification() {
        _subApplications = new ArrayList<NetworkApplicationInfo>();
        _executionFlow = new ArrayList<ExecutionFlowNode>();
    }

    public List<NetworkApplicationInfo> GetSubApplications() {
        return _subApplications;
    }

    public List<ExecutionFlowNode> GetExecutionFlow() {
        return _executionFlow;
    }
}
