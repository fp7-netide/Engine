
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
    PACKET_IN("packetIn", "PACKET_IN"),
    @XmlEnumValue("flowMod")
    FLOW_MOD("flowMod", "FLOW_MOD"),
    @XmlEnumValue("connectionUp")
    CONNECTION_UP("connectionUp", "CONNECTION_UP"),
    @XmlEnumValue("connectionDown")
    CONNECTION_DOWN("connectionDown", "CONNECTION_DOWN"),
    @XmlEnumValue("portStatus")
    PORT_STATUS("portStatus", "PORT_STATUS"),
    @XmlEnumValue("flowRemoved")
    FLOW_REMOVED("flowRemoved", "FLOW_REMOVED"),
    @XmlEnumValue("errorIn")
    ERROR_IN("errorIn", "ERROR_IN"),
    @XmlEnumValue("barrierIn")
    BARRIER_IN("barrierIn", "BARRIER_IN");
    private final String value;
    private final String value2;

    Events(String v, String v2) {
        value = v;
        value2 = v2;
    }

    public String value() {
        return value;
    }

    public static Events fromValue(String v) {
        for (Events c : Events.values()) {
            if (c.value.equals(v) || c.value2.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
