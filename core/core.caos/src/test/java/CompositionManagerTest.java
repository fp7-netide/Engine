import eu.netide.core.api.Constants;
import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.RequestResult;
import eu.netide.core.caos.CompositionManager;
import eu.netide.lib.netip.*;
import org.mockito.Mockito;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.TransportPort;
import org.testng.annotations.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by timvi on 21.09.2015.
 */
public class CompositionManagerTest {

    private static final String SingleCallXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<CompositionSpecification  xmlns=\"http://netide.eu/schemas/compositionspecification/v1\">\n" +
            "  <Modules>\n" +
            "    <Module id=\"fw\" loaderIdentification=\"ryu-fw.py\"/>\n" +
            "  </Modules>\n" +
            "  <Composition>\n" +
            "\t<ModuleCall module=\"fw\"/>\n" +
            "  </Composition>\n" +
            "</CompositionSpecification>";

    private static final OpenFlowMessage ofm1, ofm2;

    static {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_10);
        // FM1: TCP_DST:80 -> TpDst:123
        Match match1 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm1 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpDst(TransportPort.of(123))).collect(Collectors.toList()))
                .setMatch(match1)
                .build();
        ofm1 = messageFromFlowMod(offm1);
        // FM2: TCP_DST:80 -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        ofm2 = messageFromFlowMod(offm2);
    }

    @Test
    public void TestSingleNodeComposition() throws InterruptedException {
        // create PacketIn
        Ethernet ethernet = new Ethernet().setEtherType(Ethernet.TYPE_IPV4).setSourceMACAddress(MacAddress.valueOf(42)).setDestinationMACAddress(MacAddress.valueOf(43));
        IPv4 iPv4 = new IPv4().setProtocol(IPv4.PROTOCOL_TCP);
        TCP tcp = new TCP().setSourcePort((short) 80).setDestinationPort((short) 80);
        iPv4.setPayload(tcp);
        ethernet.setPayload(iPv4);
        OFPacketIn newPacketIn = OFFactories.getFactory(OFVersion.OF_10).buildPacketIn().setData(ethernet.serialize()).setReason(OFPacketInReason.NO_MATCH).build();
        OpenFlowMessage newMessage = new OpenFlowMessage();
        newMessage.setOfMessage(newPacketIn);

        // create and mock objects
        CompositionManager manager = new CompositionManager();
        IBackendManager backendManager = Mockito.mock(IBackendManager.class);
        Mockito.when(backendManager.getModules()).thenReturn(Stream.of("fw"));
        RequestResult result = new RequestResult(ofm1);
        result.addResultMessage(ofm1);
        result.signalIsDone(new ManagementMessage());
        Mockito.when(backendManager.sendRequest(Mockito.any(Message.class))).thenReturn(result);
        manager.setBackendManager(backendManager);
        manager.setCompositionSpecificationXml(SingleCallXml);
        Thread.sleep(1000); // wait for reconfiguration

        manager.OnShimMessage(newMessage, Constants.SHIM);
    }

    private static OpenFlowMessage messageFromFlowMod(OFFlowMod flowMod) {
        OpenFlowMessage newMessage = new OpenFlowMessage();
        newMessage.setOfMessage(flowMod);
        newMessage.setHeader(NetIPUtils.StubHeaderFromPayload(newMessage.getPayload()));
        newMessage.getHeader().setMessageType(MessageType.OPENFLOW);
        newMessage.getHeader().setModuleId(0); // TODO core indicator module id
        newMessage.getHeader().setDatapathId(0); // TODO calculate new value
        newMessage.getHeader().setTransactionId(0); // TODO calculate new value
        return newMessage;
    }
}
