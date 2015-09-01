
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "ParallelCall", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class ParallelCall extends ExecutionFlowNode {

    protected List<ModuleCall> moduleCalls;
    protected ResolutionPolicy resolutionPolicy;

    @XmlElement(name = "ModuleCall", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = true)
    public List<ModuleCall> getModuleCalls() {
        return this.moduleCalls;
    }

    public void setModuleCalls(List<ModuleCall> moduleCalls) {
        this.moduleCalls = moduleCalls;
    }

    @XmlAttribute(name = "resolutionPolicy")
    public ResolutionPolicy getResolutionPolicy() {
        if (resolutionPolicy == null) {
            return ResolutionPolicy.IGNORE;
        } else {
            return resolutionPolicy;
        }
    }

    public void setResolutionPolicy(ResolutionPolicy value) {
        this.resolutionPolicy = value;
    }
}
