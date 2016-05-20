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
     * Tests adding FlowMods to the FIBManager.
     *
     * Adds FlowMods allowing bidirectional communication between 00:00:00:00:00:01 and 00:00:00:00:00:0a.
     */
    @Test
    public void TestAddFlowMod() {
        final int inPort = 1;
        final int outPort = 2;

        OFFactory fact = OFFactories.getFactory(OFVersion.OF_13);

        Match match1 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IN_PORT, OFPort.of(inPort))
                .setExact(MatchField.ETH_SRC, MacAddress.of("00:00:00:00:00:01"))
                .setExact(MatchField.ETH_DST, MacAddress.of("00:00:00:00:00:0a")).build();
        // Note: 0xffef is the maximum value for this field
        OFAction action1 = fact.actions().output(OFPort.of(outPort), 0xffef);
        OFFlowMod offm1 = fact.buildFlowAdd()
                .setActions(Stream.of(action1).collect(Collectors.toList()))
                .setMatch(match1)
                .build();
        OpenFlowMessage message1 = messageFromOFMessage(offm1);

        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IN_PORT, OFPort.of(outPort))
                .setExact(MatchField.ETH_SRC, MacAddress.of("00:00:00:00:00:0a"))
                .setExact(MatchField.ETH_DST, MacAddress.of("00:00:00:00:00:01")).build();
        // Note: 0xffef is the maximum value for this field
        OFAction action2 = fact.actions().output(OFPort.of(inPort), 0xffef);
        OFFlowMod offm2 = fact.buildFlowAdd()
                .setActions(Stream.of(action2).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage message2 = messageFromOFMessage(offm2);

        fibManager.handleResult(message1);
        fibManager.handleResult(message2);
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
