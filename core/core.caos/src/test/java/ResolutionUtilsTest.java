import eu.netide.core.caos.resolution.ResolutionUtils;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by timvi on 24.08.2015.
 */
public class ResolutionUtilsTest {

    @Test
    public void TestIPV4SrcConflict() {
        Match m1 = OFFactories.getFactory(OFVersion.OF_13).buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4).setExact(MatchField.IPV4_SRC, IPv4Address.of("192.168.1.1")).build();
        Match m2 = OFFactories.getFactory(OFVersion.OF_13).buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4).setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("192.168.0.0/16")).build();

        Assert.assertTrue(ResolutionUtils.getMatchConflicts(null, null, m1, m2).count() == 2);
    }

    @Test
    public void TestActionConflict() {
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_10);
        Match match1 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm1 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match1)
                .build();
        Match match2 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm2 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpSrc(TransportPort.of(80))).collect(Collectors.toList()))
                .setMatch(match2)
                .build();

        Assert.assertEquals(offm1.getActions().get(0), offm2.getActions().get(0));

        Assert.assertTrue(ResolutionUtils.getActionConflicts(offm1, offm2));
    }
}
