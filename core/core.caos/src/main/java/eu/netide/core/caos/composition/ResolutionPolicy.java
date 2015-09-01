
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * ResolutionPolicy-enumeration.
 */
@XmlType(name = "ResolutionPolicy", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlEnum
public enum ResolutionPolicy {

    @XmlEnumValue("auto")
    AUTO("auto"),
    @XmlEnumValue("priority")
    PRIORITY("priority"),
    @XmlEnumValue("ignore")
    IGNORE("ignore");

    private final String value;

    ResolutionPolicy(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ResolutionPolicy fromValue(String v) {
        for (ResolutionPolicy c : ResolutionPolicy.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
