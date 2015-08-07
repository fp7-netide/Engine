
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für events.
 * <p>
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;simpleType name="events">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="packetIn"/>
 *     &lt;enumeration value="flowMod"/>
 *     &lt;enumeration value="connectionUp"/>
 *     &lt;enumeration value="connectionDown"/>
 *     &lt;enumeration value="portStatus"/>
 *     &lt;enumeration value="flowRemoved"/>
 *     &lt;enumeration value="errorIn"/>
 *     &lt;enumeration value="barrierIn"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "Events", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlEnum
public enum Events {

    @XmlEnumValue("packetIn")
    PACKET_IN("packetIn"),
    @XmlEnumValue("flowMod")
    FLOW_MOD("flowMod"),
    @XmlEnumValue("connectionUp")
    CONNECTION_UP("connectionUp"),
    @XmlEnumValue("connectionDown")
    CONNECTION_DOWN("connectionDown"),
    @XmlEnumValue("portStatus")
    PORT_STATUS("portStatus"),
    @XmlEnumValue("flowRemoved")
    FLOW_REMOVED("flowRemoved"),
    @XmlEnumValue("errorIn")
    ERROR_IN("errorIn"),
    @XmlEnumValue("barrierIn")
    BARRIER_IN("barrierIn");
    private final String value;

    Events(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Events fromValue(String v) {
        for (Events c : Events.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
