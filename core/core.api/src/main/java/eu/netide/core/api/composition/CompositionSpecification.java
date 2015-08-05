
package eu.netide.core.api.composition;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", propOrder = {"modules", "composition"})
@XmlRootElement(name = "CompositionSpecification", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class CompositionSpecification {
    private List<Module> modules;
    private List<ExecutionFlowNode> composition;

    @XmlElementWrapper(name = "Modules", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = true)
    @XmlElement(name = "Module", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = true)
    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> value) {
        this.modules = value;
    }

    @XmlElementWrapper(name = "Composition", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = true)
    @XmlElementRef(required = true, namespace = "http://netide.eu/schemas/compositionspecification/v1")
    public List<ExecutionFlowNode> getComposition() {
        return composition;
    }

    public void setComposition(List<ExecutionFlowNode> value) {
        this.composition = value;
    }

}
