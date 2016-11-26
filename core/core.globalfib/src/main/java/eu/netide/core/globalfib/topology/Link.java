package eu.netide.core.globalfib.topology;

import javax.xml.bind.annotation.*;

/**
 * Created by msp on 7/8/16.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "")
@XmlRootElement(name = "Link", namespace = "http://netide.eu/schemas/topologyspecification/v1")
public class Link {

    protected String destination;

    protected int destinationPort;

    protected String source;

    protected int sourcePort;

    @XmlAttribute(name = "dst", required = true)
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @XmlAttribute(name = "dst_port", required = true)
    public int getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    @XmlAttribute(name = "src", required = true)
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @XmlAttribute(name = "src_port", required = true)
    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }
}

