
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;


/**
 * <p>Java-Klasse für anonymous complex type.
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://netide.eu/schemas/compositionspecification/v1}ExecutionFlowNodeContainer"&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlRootElement(name = "Else", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class Else extends ExecutionFlowNodeContainer {

    @Override
    public String toString() {
        return "Else [flowNodes={" + Arrays.toString(this.getFlowNodes().toArray()) + "}]";
    }
}
