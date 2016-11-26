package eu.netide.core.globalfib.topology;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by msp on 7/8/16.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", propOrder = {"hosts", "switches", "links"})
@XmlRootElement(name = "Topology", namespace = "http://netide.eu/schemas/topologyspecification/v1")
public class TopologySpecification {

    private List<Host> hosts;

    private List<Switch> switches;

    private List<Link> links;

    public TopologySpecification() {
        this.hosts = new ArrayList<>();
        this.switches = new ArrayList<>();
        this.links = new ArrayList<>();
    }

    @XmlElementWrapper(name = "Hosts", namespace = "http://netide.eu/schemas/topologyspecification/v1", required=true)
    @XmlElement(name = "Host", namespace = "http://netide.eu/schemas/topologyspecification/v1", required=true)
    public List<Host> getHosts() {
        return hosts;
    }

    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    @XmlElementWrapper(name = "Switches", namespace = "http://netide.eu/schemas/topologyspecification/v1", required=true)
    @XmlElement(name = "Switch", namespace = "http://netide.eu/schemas/topologyspecification/v1", required=true)
    public List<Switch> getSwitches() {
        return switches;
    }

    public void setSwitches(List<Switch> switches) {
        this.switches = switches;
    }

    @XmlElementWrapper(name = "Links", namespace = "http://netide.eu/schemas/topologyspecification/v1", required=true)
    @XmlElement(name = "Link", namespace = "http://netide.eu/schemas/topologyspecification/v1", required=true)
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    /**
     * Creates an instance of TopologySpecification from the content of an XML specification.
     * @param topologySpecificationXML String containing the content of an XML topology specification.
     * @return TopologySpecification corresponding to the XML specification.
     */
    public static TopologySpecification topologySpecification(String topologySpecificationXML) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(TopologySpecification.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        return (TopologySpecification) unmarshaller.unmarshal(new StringReader(topologySpecificationXML));
    }

    public Set<Link> getAdjacentLinks(String id) {
        Set<Link> adjacentLinks = new HashSet<>();
        for (Link link : links) {
            if (link.getDestination().equals(id) || link.getSource().equals(id)) {
                adjacentLinks.add(link);
            }
        }
        return adjacentLinks;
    }

    public Switch getSwitch(String id) {
        for (Switch sw : switches) {
            if (sw.getId().equals(id)) {
                return sw;
            }
        }
        return null;
    }
}