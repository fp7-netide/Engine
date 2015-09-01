
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "ModuleCall", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class ModuleCall extends ExecutionFlowNode {
    private Condition callCondition;
    private Module module;
    private Boolean allowVetoResult;
    private int priority;

    @XmlElement(name = "CallCondition", namespace = "http://netide.eu/schemas/compositionspecification/v1")
    public Condition getCallCondition() {
        return callCondition;
    }

    public void setCallCondition(Condition value) {
        this.callCondition = value;
    }

    @XmlAttribute(name = "module", required = true)
    @XmlIDREF
    public Module getModule() {
        return module;
    }

    public void setModule(Module value) {
        this.module = value;
    }

    @XmlAttribute(name = "allowVetoResult", required = false)
    public Boolean getAllowVetoResult() {
        return this.allowVetoResult;
    }

    public void setAllowVetoResult(Boolean value) {
        this.allowVetoResult = value;
    }

    @XmlAttribute(name = "priority", required = false)
    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
