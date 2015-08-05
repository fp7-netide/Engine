
package eu.netide.core.api.composition;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "ModuleCall", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class ModuleCall extends ExecutionFlowNode {
    private Filter callFilter;
    private Module module;
    private Boolean allowVetoResult;

    @XmlElement(name = "CallFilter", namespace = "http://netide.eu/schemas/compositionspecification/v1")
    public Filter getCallFilter() {
        return callFilter;
    }

    public void setCallFilter(Filter value) {
        this.callFilter = value;
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

}
