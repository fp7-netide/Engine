package eu.netide.core.globalfib;

import org.projectfloodlight.openflow.protocol.match.MatchField;

/**
 * Created by arne on 09.09.15.
 */
public class Constants {
    public static final MatchField[] SUPPORTED_FIELDS = new MatchField[]{
            MatchField.IPV4_SRC,
            MatchField.IPV4_DST,
            MatchField.IPV6_SRC,
            MatchField.IPV6_DST,
            MatchField.ETH_SRC,
            MatchField.ETH_DST,
            MatchField.IN_PORT,
            MatchField.IP_PROTO,
            MatchField.ETH_TYPE,
            MatchField.TCP_SRC,
            MatchField.TCP_DST,
            MatchField.UDP_SRC,
            MatchField.UDP_DST,
            MatchField.VLAN_VID
    };
}
