
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.*;
import java.util.List;


@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ExecutionFlowNodeContainer", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlSeeAlso({
        Else.class,
        If.class
})
public abstract class ExecutionFlowNodeContainer {
    private List<ExecutionFlowNode> flowNodes;

    @XmlElementRef(required = true, namespace = "http://netide.eu/schemas/compositionspecification/v1")
    public void setFlowNodes(List<ExecutionFlowNode> flowNodes) {
        this.flowNodes = flowNodes;
    }

    public List<ExecutionFlowNode> getFlowNodes() {
        return this.flowNodes;
    }

}
