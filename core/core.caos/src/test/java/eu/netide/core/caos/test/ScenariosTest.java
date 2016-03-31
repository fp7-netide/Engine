package eu.netide.core.caos.test;

import eu.netide.core.api.IConnectorListener;
import eu.netide.core.caos.composition.CompositionSpecification;
import eu.netide.core.caos.composition.CompositionSpecificationLoader;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.execution.FlowExecutors;
import eu.netide.core.connectivity.BackendManager;
import eu.netide.core.connectivity.ShimManager;
import eu.netide.lib.netip.*;
import org.onlab.packet.*;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.internal.collections.Pair;

import javax.xml.bind.JAXBException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.*;

/**
 * Created by arne on 14.01.16.
 */
public class ScenariosTest {

    private static final String SW_ID = "SimpleSwitch";
    private static final String FW_ID = "Firewall";
    private BackendManager backendManager;
    private static final String SW_BE_ID = "MyOwnMockSwitch";
    private static final String FW_BE_ID = "EuqallyFakeFirewall";
    private Future<ExecutionFlowStatus> statusFuture;
    private FakeBackEndConnector backendConnector;
    private CompositionSpecification comppsec;
    private ShimManager shimManager;
    private ExecutorService pool;
    private FakeShimConnector shimConnector;

    @Test
    public void testNormal() throws JAXBException, InterruptedException, ExecutionException, TimeoutException {
        setupComposition();

        // Blockcall

        sendPacketIn(2, 780);


        // packet in is 2, generate msg for dpid 2 handled by sw;
        int swModId = backendManager.getModuleId(SW_ID);

        int xid = extractXidFromFirstMsgToBackend(SW_BE_ID);


        backendManager.OnDataReceived(getFlowModMsg(xid, 2, swModId).toByteRepresentation(), SW_BE_ID);


        // Force finish of module call
        backendManager.markModuleAllOutstandingRequestsAsFinished(swModId);

        ExecutionFlowStatus statusResult = statusFuture.get(1000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(statusResult.getResultMessages().size() >= 1);

    }

    private int extractXidFromFirstMsgToBackend(String beId) throws InterruptedException {
        synchronized (backendConnector.msg) {
            backendConnector.msg.wait(1000);
        }
        Assert.assertTrue(backendConnector.msg.size() >= 1);
        Assert.assertEquals(backendConnector.msg.get(0).first(), beId);

        return backendConnector.msg.get(0).second().getHeader().getTransactionId();

    }

    private void sendPacketIn(int dpid, int packetInXid) {
        OpenFlowMessage packetIn = getMsgTestPacketIn(dpid, packetInXid);
        final ExecutionFlowStatus status = new ExecutionFlowStatus(packetIn);
        statusFuture = pool.submit(() -> FlowExecutors.SEQUENTIAL.executeFlow(status, comppsec.getComposition().stream(), shimManager, backendManager));
        // Call is block and waiting for the called modules to finish
    }

    @Test
    public void testMultipleFlowMods() throws InterruptedException, JAXBException {
        setupComposition();
        int swModId = backendManager.getModuleId(SW_ID);
        int fwModId = backendManager.getModuleId(FW_ID);

        sendPacketIn(2, 780);
        backendManager.getModules().forEach(s -> {
            int id = backendManager.getModuleId(s);
            backendManager.markModuleAllOutstandingRequestsAsFinished(id);
        });
        sendPacketIn(3, 785);

        int xid = extractXidFromFirstMsgToBackend(SW_BE_ID);

        backendManager.OnDataReceived(getFlowModMsg(xid, 2, fwModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getFlowModMsg(xid, 2, swModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getFlowModMsg(xid, 2, swModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getFlowModMsg(xid, 3, fwModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getFlowModMsg(xid, 2, swModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getFlowModMsg(xid, 3, fwModId).toByteRepresentation(), SW_BE_ID);

        sendPacketIn(3, 780);
        backendManager.markModuleAllOutstandingRequestsAsFinished(swModId);
        sendPacketIn(2, 780);

    }

    @Test
    public void testTwoFlowModsPacketOUt() throws InterruptedException, JAXBException, TimeoutException, ExecutionException {
        setupComposition();
        int swModId = backendManager.getModuleId(SW_ID);

        int dpid=2;
        sendPacketIn(dpid, 800);

        int xid = extractXidFromFirstMsgToBackend(SW_BE_ID);

        backendManager.OnDataReceived(getFlowModMsg(xid, dpid, swModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getPacketOutMsg(xid, dpid, swModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getFlowModMsg(xid, dpid, swModId).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(getFenceMessage(xid, dpid, swModId), SW_BE_ID);

        ExecutionFlowStatus statusResult = statusFuture.get(1000, TimeUnit.MILLISECONDS);
        Map<Long,List<Message>> results = statusResult.getResultMessages();
        Assert.assertTrue(results.get((long) dpid).size()==3);


    }



    @Test
    public void signalisDoneMultipleTimes() throws JAXBException, InterruptedException {
        setupComposition();

        sendPacketIn(2, 782);

        for (int i=0;i<20;i++) {
            backendManager.getModules().forEach(s -> {
                int id = backendManager.getModuleId(s);
                backendManager.markModuleAllOutstandingRequestsAsFinished(id);
            });
            if (i==11)
                Thread.sleep(50);
        }

    }

    /**
     * Created by arne on 14.01.16.
     */
    public class FakeBackEndConnector implements eu.netide.core.api.IBackendConnector {

        final Vector<Pair<String, Message>> msg = new Vector<>();

        @Override
        public boolean SendData(byte[] data, String destinationId) {
            synchronized (msg) {
                msg.add(Pair.create(destinationId, NetIPConverter.parseConcreteMessage(data)));
                msg.notifyAll();
            }
            return true;
        }

        @Override
        public void RegisterBackendListener(IConnectorListener listener) {

        }
    }

    @Test
    public void basicSetupTest() throws JAXBException, InterruptedException {
        setupComposition();
    }


    private void setupComposition() throws JAXBException, InterruptedException {

        comppsec = CompositionSpecificationLoader.Load(DPIPSPec);
        pool = Executors.newFixedThreadPool(2);


        shimManager = new ShimManager();
        backendManager = new BackendManager();


        backendConnector = new FakeBackEndConnector();
        shimConnector = new FakeShimConnector();

        backendManager.setConnector(backendConnector);
        backendManager.setBackendMessageListeners(new LinkedList<>());

        // Add modules to backend
        backendManager.OnDataReceived(generateModuleAnnouncment(SW_ID).toByteRepresentation(), SW_BE_ID);
        backendManager.OnDataReceived(generateModuleAnnouncment(FW_ID).toByteRepresentation(), FW_BE_ID);

        Assert.assertSame(backendConnector.msg.size(), 2);
        Assert.assertTrue(backendConnector.msg.get(0).second() instanceof ModuleAcknowledgeMessage);
        Assert.assertTrue(backendConnector.msg.get(1).second() instanceof ModuleAcknowledgeMessage);
        backendConnector.msg.clear();
    }

    final String DPIPSPec = "<?xml version=\"1.0\" ?>\n" +
            "<CompositionSpecification\n" +
            "        xmlns=\"http://netide.eu/schemas/compositionspecification/v1\">\n" +
            "    <Modules>\n" +
            "        <Module id=\"SimpleSwitch\" loaderIdentification=\"simple_switch.py\"\n" +
            "                noFenceSupport=\"true\">\n" +
            "            <CallCondition events=\"packetIn\" datapaths=\"1 2 4\"/>\n" +
            "        </Module>\n" +
            "        <Module id=\"Firewall\" loaderIdentification=\"simple_firewall.py\"\n" +
            "                noFenceSupport=\"true\">\n" +
            "            <CallCondition events=\"packetIn\" datapaths=\"3\"/>\n" +
            "        </Module>\n" +
            "    </Modules>\n" +
            "    <Composition>\n" +
            "        <ParallelCall resolutionPolicy=\"priority\">\n" +
            "            <ModuleCall module=\"Firewall\" priority=\"1\"/>\n" +
            "            <ModuleCall module=\"SimpleSwitch\" priority=\"2\"/>\n" +
            "        </ParallelCall>\n" +
            "    </Composition>\n" +
            "</CompositionSpecification>\n";

    OpenFlowMessage getMsgTestPacketIn(long dpid, int xid) {
        Ethernet ethernet = new Ethernet().setEtherType(Ethernet.TYPE_IPV4).setSourceMACAddress(MacAddress.valueOf(42)).setDestinationMACAddress(MacAddress.valueOf(0xdd43));
        IPv4 iPv4 = new IPv4().setProtocol(IPv4.PROTOCOL_TCP);
        TCP tcp = new TCP().setSourcePort((short) 1000).setDestinationPort((short) 2000);
        tcp.setPayload(new Data(new byte[]{0x1a, 0x1b, 0x1c, 0x1d, 0x1e}));
        iPv4.setPayload(tcp);
        ethernet.setPayload(iPv4);
        OFPacketIn newPacketIn = OFFactories.getFactory(OFVersion.OF_10).buildPacketIn().setData(ethernet.serialize()).setReason(OFPacketInReason.NO_MATCH).build();
        OpenFlowMessage newMessage = new OpenFlowMessage();
        newMessage.setOfMessage(newPacketIn);
        MessageHeader header = new MessageHeader();
        header.setTransactionId(xid);

        newMessage.getHeader().setDatapathId(dpid);
        return newMessage;
    }


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    Message getFlowModMsg(int xid, long dpid, int modId) {
        String flow_mod_str = "010e0050424db09e003ffff60001000000000000e6402675839c0000000000000000000000000000000000000000000000000000000000000000000000008000ffffffffffff0001000000080002ffe5";
        return getByteArrayToMessage(xid, dpid, modId, flow_mod_str);

    }

    private Message getByteArrayToMessage(int xid, long dpid, int modId, String flow_mod_str) {
        byte[] flow_mod = hexStringToByteArray(flow_mod_str);
        MessageHeader header = new MessageHeader();
        header.setDatapathId(dpid);
        header.setMessageType(MessageType.OPENFLOW);
        header.setModuleId(modId);
        header.setTransactionId(xid);
        return new Message(header, flow_mod);
    }

    private byte[] getFenceMessage(int xid, int dpid, int modId) {
        MessageHeader header = new MessageHeader();
        header.setDatapathId(dpid);
        header.setMessageType(MessageType.FENCE);
        header.setModuleId(modId);
        header.setTransactionId(xid);
        return new Message(header, new byte[]{}).toByteRepresentation();
    }

    Message getPacketOutMsg(int xid, long dpid, int modId) {
        // With buffer id, no data
        String packetOut = "040d0028866f3059000002c10000000200100000000000000000001000000001ffe5000000000000";
        return getByteArrayToMessage(xid, dpid, modId, packetOut);
    }


    private ModuleAnnouncementMessage generateModuleAnnouncment(String moduleName) {
        ModuleAnnouncementMessage msg = new ModuleAnnouncementMessage();
        msg.setModuleName(moduleName);
        return msg;
    }

}
