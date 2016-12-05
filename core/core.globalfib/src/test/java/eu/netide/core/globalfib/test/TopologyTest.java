package eu.netide.core.globalfib.test;

import eu.netide.core.globalfib.topology.*;
import eu.netide.core.globalfib.topology.Host;
import eu.netide.core.globalfib.topology.Link;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.*;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Created by msp on 7/11/16.
 */
public class TopologyTest {

    private TopologySpecification topologySpecification;

    private String topologySpecificationXML;

    @BeforeTest
    public void setup() throws IOException {
        topologySpecificationXML = TestTopologies.small;
    }

    /**
     * Tests the creating the TopologySpecification from the specification XML file.
     * @throws JAXBException
     */
    @Test
    public void TestTopologySpecification() throws JAXBException {
        topologySpecification = TopologySpecification.topologySpecification(topologySpecificationXML);

        // Test hosts
        Assert.assertEquals(topologySpecification.getHosts().size(), 2);
        Host h1 = topologySpecification.getHosts().get(0);
        Assert.assertEquals(h1.getId(), "h1");
        Assert.assertEquals(h1.getIp(), "10.0.0.1");
        Assert.assertEquals(h1.getMac(), "00:00:00:00:00:01");

        // Test Switches
        Assert.assertEquals(topologySpecification.getSwitches().size(), 2);
        Switch s3 = topologySpecification.getSwitches().get(0);
        Assert.assertEquals(s3.getId(), "s3");
        Assert.assertEquals(s3.getDpid(), "0000000000000003");

        // Test Links
        Assert.assertEquals(topologySpecification.getLinks().size(), 3);
        Link l1 = topologySpecification.getLinks().get(0);
        Assert.assertEquals(l1.getDestination(), "s4");
        Assert.assertEquals(l1.getDestinationPort(), 1);
        Assert.assertEquals(l1.getSource(), "s3");
        Assert.assertEquals(l1.getSourcePort(), 2);
    }

    /**
     * Tests the TopologyManager.
     */
    @Test(dependsOnMethods = {"TestTopologySpecification"})
    public void TestTopologyManager() {
        TopologyManager topologyManager = new TopologyManager();
        topologyManager.setTopologySpecification(topologySpecification);

        Topology topology = topologyManager.currentTopology();
        Assert.assertNotNull(topology);

        TopologyGraph topologyGraph = topologyManager.getGraph(topology);
        Assert.assertEquals(topologyGraph.getVertexes().size(), 2);
        Assert.assertEquals(topologyGraph.getEdges().size(), 2);

        TopologyVertex s3 = (TopologyVertex) topologyGraph.getVertexes().toArray()[0];
        Assert.assertTrue(s3.deviceId().equals(
                DeviceId.deviceId("of:0000000000000003")
        ));

        TopologyEdge l1 = (TopologyEdge) topologyGraph.getEdges().toArray()[0];
        Assert.assertTrue(l1.link().dst().equals(
                new ConnectPoint(
                        DeviceId.deviceId("of:0000000000000004"),
                        PortNumber.portNumber(1)
                )
        ));
        Assert.assertTrue(l1.link().src().equals(
                new ConnectPoint(
                        DeviceId.deviceId("of:0000000000000003"),
                        PortNumber.portNumber(2)
                )
        ));
    }

    /**
     * Tests the HostManager.
     */
    @Test(dependsOnMethods = {"TestTopologySpecification"})
    public void TestHostManager() {
        HostManager hostManager = new HostManager();
        hostManager.setTopologySpecification(topologySpecification);

        Assert.assertEquals(hostManager.getHostCount(), 2);
        Iterable<org.onosproject.net.Host> hosts = hostManager.getHosts();

        // Test getting invalid host
        HostId h3 = HostId.hostId(MacAddress.valueOf("00:00:00:00:00:03"));
        org.onosproject.net.Host host3 = hostManager.getHost(h3);
        Assert.assertNull(host3);

        // Test if fields of hosts are set correctly
        HostId h1 = HostId.hostId(MacAddress.valueOf("00:00:00:00:00:01"));
        org.onosproject.net.Host host1 = hostManager.getHost(h1);
        Assert.assertNotNull(host1);
        Assert.assertEquals(host1.ipAddresses().iterator().next(), IpAddress.valueOf("10.0.0.1"));
        Assert.assertEquals(host1.location().deviceId(), (DeviceId.deviceId("of:0000000000000003")));
    }

    /**
     * Tests the getPath function of the TopologyManager
     */
    @Test(dependsOnMethods = {"TestTopologySpecification"})
    public void TestGetPath() throws JAXBException, IOException {
        String specificationXML = TestTopologies.big;
        TopologySpecification specification = TopologySpecification.topologySpecification(specificationXML);
        TopologyManager topologyManager = new TopologyManager();
        topologyManager.setTopologySpecification(specification);

        DeviceId src = DeviceId.deviceId("of:0000000000000001");
        DeviceId dst = DeviceId.deviceId("of:0000000000000004");
        Set<org.onosproject.net.Path> paths = topologyManager.getPaths(topologyManager.currentTopology(), src, dst);
        Assert.assertNotEquals(paths.size(), 0);
    }
}
