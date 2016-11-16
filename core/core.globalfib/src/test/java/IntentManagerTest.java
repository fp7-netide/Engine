import eu.netide.core.globalfib.FlowModEntry;
import eu.netide.core.globalfib.intent.HostToHostIntent;
import eu.netide.core.globalfib.intent.Intent;
import eu.netide.core.globalfib.intent.IntentManager;
import eu.netide.core.globalfib.topology.HostManager;
import eu.netide.core.globalfib.topology.TopologyManager;
import eu.netide.core.globalfib.topology.TopologySpecification;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by msp on 11/9/16.
 */
public class IntentManagerTest {

    HostManager hostManager = new HostManager();
    TopologyManager topologyManager = new TopologyManager();

    @BeforeTest
    public void setup() throws IOException, JAXBException {
        Path path = Paths.get("core.globalfib/src/test/test_topology_big.xml").toAbsolutePath();
        String topologySpecificationXML = new String(Files.readAllBytes(path));

        TopologySpecification topologySpecification =
                TopologySpecification.topologySpecification(topologySpecificationXML);
        hostManager.setTopologySpecification(topologySpecification);
        topologyManager.setTopologySpecification(topologySpecification);
    }

    private OFFlowMod createFlowMod(String ethDst, int inPort, int outPort) {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_13);

        Match match = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IN_PORT, OFPort.of(inPort))
                .setExact(MatchField.ETH_DST, MacAddress.of(ethDst)).build();
        OFAction action = fact.actions().output(OFPort.of(outPort), 65509);
        return fact.buildFlowAdd()
                .setXid(19)
                .setIdleTimeout(5)
                .setPriority(10)
                .setBufferId(OFBufferId.of(268))
                .setMatch(match)
                .setActions(Stream.of(action).collect(Collectors.toList()))
                .build();
    }

    /**
     * Tests the process function.
     */
    @Test
    public void TestProcess() {
        IntentManager intentManager = new IntentManager();
        intentManager.bindHostService(hostManager);
        intentManager.bindTopologyService(topologyManager);

        int moduleId = 1;
        int inPort = 1;
        int outPort = 3;
        String dstMac = "00:00:00:00:00:06";
        OFFlowMod flowMod = createFlowMod(dstMac, inPort, outPort);
        FlowModEntry flowModEntry = new FlowModEntry(flowMod, 1, moduleId);
        intentManager.process(flowModEntry);

        // Get new intent
        Intent intent = intentManager.getIntents().iterator().next();
        Assert.assertNotNull(intent);
        Assert.assertTrue(intent instanceof HostToHostIntent);

        // Test fields of intent
        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(flowModEntry.getDpid()));
        PortNumber inPortNumber = PortNumber.portNumber(inPort);
        org.onlab.packet.MacAddress dstMacAddress = org.onlab.packet.MacAddress.valueOf(dstMac);

        HostToHostIntent hostToHostIntent = (HostToHostIntent) intent;
        Assert.assertEquals(deviceId, hostToHostIntent.getSource().location().deviceId());
        Assert.assertEquals(inPortNumber, hostToHostIntent.getSource().location().port());
        Assert.assertEquals(dstMacAddress, hostToHostIntent.getDestination().mac());

        Assert.assertEquals(intent.getFlowModEntries().size(), 1);
        Assert.assertEquals(flowModEntry, intent.getFlowModEntries().iterator().next());
    }

    /**
     * Make sure that the check for duplicate intents works.
     */
    @Test
    public void TestDuplicateIntents() {
        IntentManager intentManager = new IntentManager();
        intentManager.bindHostService(hostManager);
        intentManager.bindTopologyService(topologyManager);

        int moduleId = 1;
        int inPort = 1;
        int outPort = 3;
        String dstMac = "00:00:00:00:00:06";
        OFFlowMod flowMod = createFlowMod(dstMac, inPort, outPort);
        FlowModEntry flowModEntry = new FlowModEntry(flowMod, 1, moduleId);

        // Process twice!
        intentManager.process(flowModEntry);
        intentManager.process(flowModEntry);

        Assert.assertEquals(intentManager.getIntents().size(), 1);
    }

    /**
     * Make sure that intents don't add duplicate FlowMods.
     */
    @Test
    public void TestDuplicateFlowMods() {
        IntentManager intentManager = new IntentManager();
        intentManager.bindHostService(hostManager);
        intentManager.bindTopologyService(topologyManager);

        int moduleId = 1;
        String dstMac = "00:00:00:00:00:06";

        // Create new intent
        OFFlowMod flowMod1 = createFlowMod(dstMac, 1, 3);
        FlowModEntry flowModEntry1 = new FlowModEntry(flowMod1, 1, moduleId);
        intentManager.process(flowModEntry1);

        OFFlowMod flowMod2 = createFlowMod(dstMac, 1, 3);
        FlowModEntry flowModEntry2 = new FlowModEntry(flowMod2, 5, moduleId);
        intentManager.process(flowModEntry2);

        // Add duplicate of flowMod2
        OFFlowMod flowMod2dup = createFlowMod(dstMac, 1, 3);
        FlowModEntry flowModEntry2dup = new FlowModEntry(flowMod2dup, 5, moduleId);
        intentManager.process(flowModEntry2dup);

        Assert.assertEquals(intentManager.getIntents().size(), 1);
        Intent intent = intentManager.getIntents().iterator().next();

        Assert.assertEquals(intent.getFlowModEntries().size(), 2);
    }

    /**
     * Test a sequence of FlowMods for the host to host intent h1->h6 (h1-s1-s5-s7-s6-s3-h6) where the order of
     * FlowMods is not in correctly ordered. This is to test the storing of unassociated FlowMods and assigning them
     * to an intent later.
     */
    @Test
    public void TestUnorderedArival() {
        IntentManager intentManager = new IntentManager();
        intentManager.bindHostService(hostManager);
        intentManager.bindTopologyService(topologyManager);

        int moduleId = 1;
        String dstMac = "00:00:00:00:00:06";

        // First define all FlowModEntries in order
        // Path looks like this (<n> means port n)
        //      h1 --<1> s1 <3>--<1> s5 <3>--<1> s7 <2>--<3> s6 <1>--<3> s3 <2>-- h6
        OFFlowMod flowMod1 = createFlowMod(dstMac, 1, 3);
        FlowModEntry flowModEntry1 = new FlowModEntry(flowMod1, 1, moduleId);
        OFFlowMod flowMod2 = createFlowMod(dstMac, 1, 3);
        FlowModEntry flowModEntry2 = new FlowModEntry(flowMod2, 5, moduleId);
        OFFlowMod flowMod3 = createFlowMod(dstMac, 1, 2);
        FlowModEntry flowModEntry3 = new FlowModEntry(flowMod3, 7, moduleId);
        OFFlowMod flowMod4 = createFlowMod(dstMac, 3, 1);
        FlowModEntry flowModEntry4 = new FlowModEntry(flowMod4, 6, moduleId);
        OFFlowMod flowMod5 = createFlowMod(dstMac, 3, 2);
        FlowModEntry flowModEntry5 = new FlowModEntry(flowMod5, 3, moduleId);

        // Now add FlowModEntries in non-standard order
        intentManager.process(flowModEntry3);
        intentManager.process(flowModEntry5);
        // At this point no intent should have been created yet
        Assert.assertEquals(intentManager.getIntents().size(), 0);

        // Here the Intent should get initialized
        intentManager.process(flowModEntry1);
        Assert.assertEquals(intentManager.getIntents().size(), 1);

        intentManager.process(flowModEntry2);
        intentManager.process(flowModEntry4);

        Assert.assertEquals(intentManager.getIntents().size(), 1);
        HostToHostIntent intent = (HostToHostIntent) intentManager.getIntents().iterator().next();
        Assert.assertEquals(intent.getFlowModEntries().size(), 5);
    }

    /**
     * Tests the findMatchingIntents function.
     */
    @Test
    public void TestFindMatchingIntents() {
        IntentManager intentManager = new IntentManager();
        intentManager.bindHostService(hostManager);
        intentManager.bindTopologyService(topologyManager);

        // Insert a HostToHostIntent from h1 to h6 by inserting appropriate FlowMod.
        // Path looks like this (<n> means port n)
        //      h1 --<1> s1 <3>--<1> s5 <3>--<1> s7 <2>--<3> s6 <1>--<3> s3 <2>-- h6
        int moduleId = 1;
        OFFlowMod intentFlowMod = createFlowMod("00:00:00:00:00:06", 1, 3);
        FlowModEntry intentEntry = new FlowModEntry(intentFlowMod, 1, moduleId);
        intentManager.process(intentEntry);

        // Test matching FlowModEntry at s7
        OFFlowMod flowMod0 = createFlowMod("00:00:00:00:00:06", 1, 2);
        FlowModEntry matchingEntry0 = new FlowModEntry(flowMod0, 7, moduleId);
        Set<Intent> matches0 = intentManager.findMatchingIntents(matchingEntry0);
        Assert.assertEquals(matches0.size(), 1);

        // Test matching FlowModEntry at s3 (output to host)
        OFFlowMod flowMod1 = createFlowMod("00:00:00:00:00:06", 3, 2);
        FlowModEntry matchingEntry1 = new FlowModEntry(flowMod1, 3, moduleId);
        Set<Intent> matches1 = intentManager.findMatchingIntents(matchingEntry1);
        Assert.assertEquals(matches1.size(), 1);

        // Test FlowEntry with destination mac mismatch
        OFFlowMod flowMod2 = createFlowMod("00:00:00:00:00:07", 1, 2);
        FlowModEntry matchingEntry2 = new FlowModEntry(flowMod2, 7, moduleId);
        Set<Intent> matches2 = intentManager.findMatchingIntents(matchingEntry2);
        Assert.assertEquals(matches2.size(), 0);

        // Test FlowEntry with moduleId mismatch
        OFFlowMod flowMod3 = createFlowMod("00:00:00:00:00:06", 1, 2);
        FlowModEntry matchingEntry3 = new FlowModEntry(flowMod3, 7, 2);
        Set<Intent> matches3 = intentManager.findMatchingIntents(matchingEntry3);
        Assert.assertEquals(matches3.size(), 0);

        // Test FlowEntry not on intent's path (s6 <2>--<3> s4)
        OFFlowMod flowMod4 = createFlowMod("00:00:00:00:00:06", 2, 3);
        FlowModEntry matchingEntry4 = new FlowModEntry(flowMod4, 6, moduleId);
        Set<Intent> matches4 = intentManager.findMatchingIntents(matchingEntry4);
        Assert.assertEquals(matches4.size(), 0);
    }
}
