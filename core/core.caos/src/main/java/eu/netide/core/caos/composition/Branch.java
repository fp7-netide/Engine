
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", propOrder = {"if", "else"})
@XmlRootElement(name = "Branch", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class Branch extends ExecutionFlowNode {
    private If _if;
    private Else _else;

    @XmlElement(name = "If", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = true)
    public If getIf() {
        return _if;
    }

    public void setIf(If value) {
        this._if = value;
    }

    @XmlElement(name = "Else", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = true)
    public Else getElse() {
        return _else;
    }

    public void setElse(Else value) {
        this._else = value;
    }

    @Override
    public ExecutionResult Execute(ExecutionFlowStatus status) {
        return null; //TODO
    }
}
