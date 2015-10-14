import eu.netide.core.caos.composition.ResolutionPolicy;
import eu.netide.core.caos.resolution.DefaultOFConflictResolver;
import eu.netide.core.caos.resolution.PriorityInfo;
import eu.netide.core.caos.resolution.ResolutionAction;
import eu.netide.core.caos.resolution.ResolutionResult;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.TransportPort;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by timvi on 07.09.2015.
 */
public class DefaultOFConflictResolverTest {

    @Test
    public void TwoEqualRulesIgnoreTest() {
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
        OpenFlowMessage ofm1 = messageFromFlowMod(offm1);
        // FM2: TCP_DST:80 -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage ofm2 = messageFromFlowMod(offm2);

        DefaultOFConflictResolver resolver = new DefaultOFConflictResolver();
        ResolutionResult result = resolver.resolve(new Message[]{ofm1, ofm2}, ResolutionPolicy.IGNORE, null);

        Assert.assertTrue(result.getTakenActions().get(ofm1) == ResolutionAction.IGNORED);
        Assert.assertTrue(result.getTakenActions().get(ofm2) == ResolutionAction.IGNORED);
        Assert.assertEquals(result.getResultingMessagesToSend().length, 0);
    }

    @Test
    public void ThreeOverlappingRulesIgnoreTest() {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_10);
        // FM1: IPv4,TCP,TCP_DST:80 -> TpDst:123
        Match match1 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm1 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpDst(TransportPort.of(123))).collect(Collectors.toList()))
                .setMatch(match1)
                .build();
        OpenFlowMessage ofm1 = messageFromFlowMod(offm1);
        // FM2: IPv4,TCP -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage ofm2 = messageFromFlowMod(offm2);
        // FM3: IPv4,TCP,TCP_SRC:80 -> TpSrc:80
        Match match3 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_SRC, TransportPort.of(80)).build();
        OFFlowMod offm3 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(42))).collect(Collectors.toList()))
                .setMatch(match3)
                .build();
        OpenFlowMessage ofm3 = messageFromFlowMod(offm3);

        DefaultOFConflictResolver resolver = new DefaultOFConflictResolver();
        ResolutionResult result = resolver.resolve(new Message[]{ofm1, ofm2, ofm3}, ResolutionPolicy.IGNORE, null);

        Assert.assertEquals(result.getTakenActions().get(ofm1), ResolutionAction.IGNORED);
        Assert.assertEquals(result.getTakenActions().get(ofm2), ResolutionAction.IGNORED);
        Assert.assertEquals(result.getTakenActions().get(ofm3), ResolutionAction.NONE);
        Assert.assertEquals(result.getResultingMessagesToSend().length, 1);
    }

    @Test
    public void TwoEqualRulesPriorityTest() {
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
        OpenFlowMessage ofm1 = messageFromFlowMod(offm1);
        // FM2: TCP_DST:80 -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage ofm2 = messageFromFlowMod(offm2);
        ofm2.getHeader().setModuleId(1);

        PriorityInfo priorityInfo = new PriorityInfo(0);
        priorityInfo.addInfo(1, 1);

        DefaultOFConflictResolver resolver = new DefaultOFConflictResolver();
        ResolutionResult result = resolver.resolve(new Message[]{ofm1, ofm2}, ResolutionPolicy.PRIORITY, priorityInfo);

        Assert.assertTrue(result.getTakenActions().get(ofm1) == ResolutionAction.IGNORED);
        Assert.assertTrue(result.getTakenActions().get(ofm2) == ResolutionAction.NONE);
        Assert.assertEquals(result.getResultingMessagesToSend().length, 1);
    }

    @Test
    public void TwoEqualRulesTest() {
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
        OpenFlowMessage ofm1 = messageFromFlowMod(offm1);
        // FM2: TCP_DST:80 -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage ofm2 = messageFromFlowMod(offm2);

        DefaultOFConflictResolver resolver = new DefaultOFConflictResolver();
        ResolutionResult result = resolver.resolve(new Message[]{ofm1, ofm2}, ResolutionPolicy.AUTO, null);

        Assert.assertTrue(result.getTakenActions().get(ofm1) == ResolutionAction.REPLACED_AUTO);
        Assert.assertTrue(result.getTakenActions().get(ofm2) == ResolutionAction.REPLACED_AUTO);
        Assert.assertEquals(result.getResultingMessagesToSend().length, 1);
    }

    @Test
    public void ThreeEqualRulesTest() {
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
        OpenFlowMessage ofm1 = messageFromFlowMod(offm1);
        // FM2: TCP_DST:80 -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage ofm2 = messageFromFlowMod(offm2);
        // FM3: TCP_DST:80 -> TpSrc:80
        Match match3 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm3 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(42))).collect(Collectors.toList()))
                .setMatch(match3)
                .build();
        OpenFlowMessage ofm3 = messageFromFlowMod(offm3);

        DefaultOFConflictResolver resolver = new DefaultOFConflictResolver();
        ResolutionResult result = resolver.resolve(new Message[]{ofm1, ofm2, ofm3}, ResolutionPolicy.AUTO, null);

        Assert.assertTrue(result.getTakenActions().get(ofm1) == ResolutionAction.REPLACED_AUTO);
        Assert.assertTrue(result.getTakenActions().get(ofm2) == ResolutionAction.REPLACED_AUTO);
        Assert.assertTrue(result.getTakenActions().get(ofm3) == ResolutionAction.REPLACED_AUTO);
        Assert.assertEquals(result.getResultingMessagesToSend().length, 1);
    }

    @Test
    public void TwoOverlappingRulesTest() {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_10);
        // FM1: IPv4,TCP,TCP_DST:80 -> TpDst:123
        Match match1 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm1 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpDst(TransportPort.of(123))).collect(Collectors.toList()))
                .setMatch(match1)
                .build();
        OpenFlowMessage ofm1 = messageFromFlowMod(offm1);
        // FM2: IPv4,TCP -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage ofm2 = messageFromFlowMod(offm2);

        DefaultOFConflictResolver resolver = new DefaultOFConflictResolver();
        ResolutionResult result = resolver.resolve(new Message[]{ofm1, ofm2}, ResolutionPolicy.AUTO, null);

        Assert.assertEquals(result.getTakenActions().get(ofm1), ResolutionAction.REPLACED_AUTO);
        Assert.assertEquals(result.getTakenActions().get(ofm2), ResolutionAction.NONE);
        Assert.assertEquals(result.getResultingMessagesToSend().length, 2);
    }

    @Test
    public void ThreeOverlappingRulesTest() {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_10);
        // FM1: IPv4,TCP,TCP_DST:80 -> TpDst:123
        Match match1 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm1 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpDst(TransportPort.of(123))).collect(Collectors.toList()))
                .setMatch(match1)
                .build();
        OpenFlowMessage ofm1 = messageFromFlowMod(offm1);
        // FM2: IPv4,TCP -> TpSrc:80
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();
        OpenFlowMessage ofm2 = messageFromFlowMod(offm2);
        // FM3: IPv4,TCP,TCP_SRC:80 -> TpSrc:80
        Match match3 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_SRC, TransportPort.of(80)).build();
        OFFlowMod offm3 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(42))).collect(Collectors.toList()))
                .setMatch(match3)
                .build();
        OpenFlowMessage ofm3 = messageFromFlowMod(offm3);

        DefaultOFConflictResolver resolver = new DefaultOFConflictResolver();
        ResolutionResult result = resolver.resolve(new Message[]{ofm1, ofm2, ofm3}, ResolutionPolicy.AUTO, null);

        Assert.assertEquals(result.getTakenActions().get(ofm1), ResolutionAction.REPLACED_AUTO);
        Assert.assertEquals(result.getTakenActions().get(ofm2), ResolutionAction.NONE);
        Assert.assertEquals(result.getTakenActions().get(ofm3), ResolutionAction.REPLACED_AUTO);
        Assert.assertEquals(result.getResultingMessagesToSend().length, 4);
    }

    private OpenFlowMessage messageFromFlowMod(OFFlowMod flowMod) {
        OpenFlowMessage newMessage = new OpenFlowMessage();
        newMessage.setOfMessage(flowMod);
        newMessage.setHeader(NetIPUtils.StubHeaderFromPayload(newMessage.getPayload()));
        newMessage.getHeader().setMessageType(MessageType.OPENFLOW);
        newMessage.getHeader().setModuleId(0);
        newMessage.getHeader().setDatapathId(0);
        newMessage.getHeader().setTransactionId(0);
        return newMessage;
    }
}
