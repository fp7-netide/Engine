
package eu.netide.core.api.composition;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für mergePolicy.
 * <p/>
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p/>
 * <pre>
 * &lt;simpleType name="mergePolicy">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="priority"/>
 *     &lt;enumeration value="ignore"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "MergePolicy", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlEnum
public enum MergePolicy {

    @XmlEnumValue("priority")
    PRIORITY("priority"),
    @XmlEnumValue("ignore")
    IGNORE("ignore");

    private final String value;

    MergePolicy(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MergePolicy fromValue(String v) {
        for (MergePolicy c : MergePolicy.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
