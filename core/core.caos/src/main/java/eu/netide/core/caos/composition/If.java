
package eu.netide.core.caos.composition;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", namespace = "http://netide.eu/schemas/compositionspecification/v1")
@XmlRootElement(name = "If", namespace = "http://netide.eu/schemas/compositionspecification/v1")
public class If extends ExecutionFlowNodeContainer {

    protected List<Events> events;

    public List<Events> getEvents() {
        return this.events;
    }

    @XmlAttribute(name = "event")
    public void setEvents(List<Events> events) {
        this.events = events;
    }
}
