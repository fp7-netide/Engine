
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.*;


/**
 * <p>Java-Klasse f�r anonymous complex type.
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice minOccurs="0"&gt;
 *         &lt;element ref="{http://netide.eu/schemas/compositionspecification/v1}CallFilter"/&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="id" use="required" type="{http://netide.eu/schemas/compositionspecification/v1}moduleID" /&gt;
 *       &lt;attribute name="loaderIdentification" use="required" type="{http://netide.eu/schemas/compositionspecification/v1}loaderIdentification" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
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

    protected int deadTimeOut=-1;

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

    @XmlAttribute(name = "noFenceSupport")
    public boolean getFenceSupport()
    {
        return fenceSupport;
    }

    public void setFenceSupport(boolean value)
    {
        fenceSupport = ! value;
    }


    @XmlAttribute(name = "deadTimeOut")
    public int getDeadTimeOut()
    {
        return deadTimeOut;
    }

    public void setDeadTimeOut(int timeOut) {
        deadTimeOut=timeOut;
    }


    @Override
    public String toString() {
        return "Module [Name=" + id + ",loaderIdentification=" + loaderIdentification + ",CallCondition=" + (callCondition == null ? "null" : callCondition.toString()) + "]";
    }
}
