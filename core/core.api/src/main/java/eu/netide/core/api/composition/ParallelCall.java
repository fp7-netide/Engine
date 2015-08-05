
package eu.netide.core.api.composition;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "ParallelCall", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class ParallelCall extends ExecutionFlowNode {

    protected List<ModuleCall> moduleCalls;
    protected MergePolicy mergePolicy;

    @XmlElement(name = "ModuleCall", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = true)
    public List<ModuleCall> getModuleCalls() {
        return this.moduleCalls;
    }

    public void setModuleCalls(List<ModuleCall> moduleCalls) {
        this.moduleCalls = moduleCalls;
    }

    @XmlAttribute(name = "mergePolicy")
    public MergePolicy getMergePolicy() {
        if (mergePolicy == null) {
            return MergePolicy.IGNORE;
        } else {
            return mergePolicy;
        }
    }

    public void setMergePolicy(MergePolicy value) {
        this.mergePolicy = value;
    }

}
