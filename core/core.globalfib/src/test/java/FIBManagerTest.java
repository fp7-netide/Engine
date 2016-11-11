import eu.netide.core.api.IShimManager;
import eu.netide.core.globalfib.FIBManager;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.mockito.Mockito;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by msp on 1/25/16.
 */
public class FIBManagerTest {

    FIBManager fibManager;

    @BeforeTest
    public void setup() {
        fibManager = new FIBManager();

        IShimManager shimManager = Mockito.mock(IShimManager.class);
        fibManager.bindShimManager(shimManager);
    }

    /**
     * Tests handling of FlowAdd messages.
     *
     * Data taken from OFFlowAdd captured during operation.
     */
    @Test
    public void TestFlowAdd() {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_13);

        Match match = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IN_PORT, OFPort.of(1))
                .setExact(MatchField.ETH_DST, MacAddress.of("3e:d7:00:b9:ca:db")).build();
        OFAction action = fact.actions().output(OFPort.of(2), 65509);
        OFFlowAdd flowAdd = fact.buildFlowAdd()
                .setXid(19)
                .setIdleTimeout(5)
                .setPriority(10)
                .setBufferId(OFBufferId.of(268))
                .setMatch(match)
                .setActions(Stream.of(action).collect(Collectors.toList()))
                .build();

        OpenFlowMessage message = messageFromOFMessage(flowAdd);
        fibManager.handleResult(message);
    }

    /**
     * Tests handling of PacketOut messages.
     *
     * Data taken from OFPacketOut captured during operation.
     */
    @Test
    public void TestPacketOut() {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_13);

        OFAction action = fact.actions().output(OFPort.FLOOD, 65509);
        OFPacketOut packetOut = fact.buildPacketOut()
                .setXid(36)
                .setBufferId(OFBufferId.of(285))
                .setInPort(OFPort.of(2))
                .setActions(Stream.of(action).collect(Collectors.toList()))
                .build();

        OpenFlowMessage message = messageFromOFMessage(packetOut);
        fibManager.handleResult(message);
    }

    private static OpenFlowMessage messageFromOFMessage(OFMessage ofMessage) {
        OpenFlowMessage newMessage = new OpenFlowMessage();
        newMessage.setOfMessage(ofMessage);
        newMessage.setHeader(NetIPUtils.StubHeaderFromPayload(newMessage.getPayload()));
        newMessage.getHeader().setMessageType(MessageType.OPENFLOW);
        newMessage.getHeader().setModuleId(0); // TODO core indicator module id
        newMessage.getHeader().setDatapathId(0); // TODO calculate new value
        newMessage.getHeader().setTransactionId(0); // TODO calculate new value
        return newMessage;
    }
}
