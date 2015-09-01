import eu.netide.core.caos.resolution.ResolutionUtils;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.testng.Assert;
import org.testng.annotations.Test;

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
}
