
package eu.netide.core.api.composition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für anonymous complex type.
 * <p/>
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;extension base="{http://netide.eu/schemas/compositionspecification/v1}ExecutionFlowNodeContainer">
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlRootElement(name = "Else", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class Else extends ExecutionFlowNodeContainer {


}
