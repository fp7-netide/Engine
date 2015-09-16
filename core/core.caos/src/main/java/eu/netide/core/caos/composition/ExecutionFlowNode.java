
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ExecutionFlowNode", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlSeeAlso({ModuleCall.class, ParallelCall.class, Branch.class})
public abstract class ExecutionFlowNode {

    @Override
    public abstract String toString();
}
