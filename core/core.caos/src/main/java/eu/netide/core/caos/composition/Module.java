
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.*;


/**
 * <p>Java-Klasse f�r anonymous complex type.
 * <p>
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice minOccurs="0">
 *         &lt;element ref="{http://netide.eu/schemas/compositionspecification/v1}CallFilter"/>
 *       &lt;/choice>
 *       &lt;attribute name="id" use="required" type="{http://netide.eu/schemas/compositionspecification/v1}moduleID" />
 *       &lt;attribute name="loaderIdentification" use="required" type="{http://netide.eu/schemas/compositionspecification/v1}loaderIdentification" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "Module", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class Module {

    protected Condition callCondition;

    protected String id;

    protected String loaderIdentification;

    protected boolean fenceSupport;

    @XmlElement(name = "CallCondition", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = false)
    public Condition getCallCondition() {
        return callCondition;
    }

    public void setCallCondition(Condition value) {
        this.callCondition = value;
    }

    @XmlAttribute(name = "id", required = true)
    @XmlID
    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    @XmlAttribute(name = "loaderIdentification", required = true)
    public String getLoaderIdentification() {
        return loaderIdentification;
    }

    public void setLoaderIdentification(String value) {
        this.loaderIdentification = value;
    }

    @XmlAttribute(name = "noFenceSupport", namespace = "http://netide.eu/schemas/compositionspecification/v1", required = false)
    public boolean getFenceSupport()
    {
        return fenceSupport;
    }

    public void setFenceSupport(boolean value)
    {
        fenceSupport = ! value;
    }

    @Override
    public String toString() {
        return "Module [Name=" + id + ",loaderIdentification=" + loaderIdentification + ",CallCondition=" + (callCondition == null ? "null" : callCondition.toString()) + "]";
    }
}
