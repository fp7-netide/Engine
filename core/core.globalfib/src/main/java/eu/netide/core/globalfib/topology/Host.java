package eu.netide.core.globalfib.topology;

import javax.xml.bind.annotation.*;

/**
 * Created by msp on 7/8/16.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "Host", namespace = "http://netide.eu/schemas/topologyspecification/v1")
public class Host {

    protected String id;

    protected String mac;

    protected String ip;

    @XmlAttribute(name = "id", required = true)
    @XmlID
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "mac", required = true)
    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @XmlAttribute(name = "ip", required = true)
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
