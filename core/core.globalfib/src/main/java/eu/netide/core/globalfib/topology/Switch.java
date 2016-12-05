package eu.netide.core.globalfib.topology;

import javax.xml.bind.annotation.*;

/**
 * Created by msp on 7/8/16.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "Switch", namespace = "http://netide.eu/schemas/topologyspecification/v1")
public class Switch {

    protected String id;

    protected String dpid;

    @XmlAttribute(name = "id", required = true)
    @XmlID
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "dpid", required = true)
    public String getDpid() {
        return dpid;
    }

    public void setDpid(String dpid) {
        this.dpid = dpid;
    }
}
