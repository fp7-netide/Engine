
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * <p>Java-Klasse für filter complex type.
 * <p>
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;complexType name="filter">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attGroup ref="{http://netide.eu/schemas/compositionspecification/v1}callFilterAttributes"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Filter", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class Filter {

    protected List<Events> events;

    @XmlAttribute(name = "events")
    public List<Events> getEvents() {
        return this.events;
    }

    public void setEvents(List<Events> events) {
        this.events = events;
    }
}
