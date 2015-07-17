package org.onosproject.shim;

import static org.onosproject.net.flow.criteria.Criteria.matchLambda;
import static org.onosproject.net.flow.criteria.Criteria.matchOchSignalType;

import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.Lambda;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.CircuitSignalID;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U8;

public class ShimTrafficSelector {

	
    public static TrafficSelector buildSelector(Match match) {
        MacAddress mac;
        Ip4Prefix ip4Prefix;
        Ip6Address ip6Address;
        Ip6Prefix ip6Prefix;

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder();
        for (MatchField<?> field : match.getMatchFields()) {
            switch (field.id) {
            case IN_PORT:
                builder.matchInPort(PortNumber
                        .portNumber(match.get(MatchField.IN_PORT).getPortNumber()));
                break;
            case IN_PHY_PORT:
                builder.matchInPhyPort(PortNumber
                        .portNumber(match.get(MatchField.IN_PHY_PORT).getPortNumber()));
                break;
            case METADATA:
                long metadata =
                    match.get(MatchField.METADATA).getValue().getValue();
                builder.matchMetadata(metadata);
                break;
            case ETH_DST:
                mac = MacAddress.valueOf(match.get(MatchField.ETH_DST).getLong());
                builder.matchEthDst(mac);
                break;
            case ETH_SRC:
                mac = MacAddress.valueOf(match.get(MatchField.ETH_SRC).getLong());
                builder.matchEthSrc(mac);
                break;
            case ETH_TYPE:
                int ethType = match.get(MatchField.ETH_TYPE).getValue();
                if (ethType == EthType.VLAN_FRAME.getValue()) {
                    builder.matchVlanId(VlanId.ANY);
                } else {
                    builder.matchEthType((short) ethType);
                }
                break;
            case VLAN_VID:
                VlanId vlanId = null;
                if (match.isPartiallyMasked(MatchField.VLAN_VID)) {
                    Masked<OFVlanVidMatch> masked = match.getMasked(MatchField.VLAN_VID);
                    if (masked.getValue().equals(OFVlanVidMatch.PRESENT)
                            && masked.getMask().equals(OFVlanVidMatch.PRESENT)) {
                        vlanId = VlanId.ANY;
                    }
                } else {
                    vlanId = VlanId.vlanId(match.get(MatchField.VLAN_VID).getVlan());
                }
                if (vlanId != null) {
                    builder.matchVlanId(vlanId);
                }
                break;
            case VLAN_PCP:
                byte vlanPcp = match.get(MatchField.VLAN_PCP).getValue();
                builder.matchVlanPcp(vlanPcp);
                break;
            case IP_DSCP:
                byte ipDscp = match.get(MatchField.IP_DSCP).getDscpValue();
                builder.matchIPDscp(ipDscp);
                break;
            case IP_ECN:
                byte ipEcn = match.get(MatchField.IP_ECN).getEcnValue();
                builder.matchIPEcn(ipEcn);
                break;
            case IP_PROTO:
                short proto = match.get(MatchField.IP_PROTO).getIpProtocolNumber();
                builder.matchIPProtocol((byte) proto);
                break;
            case IPV4_SRC:
                if (match.isPartiallyMasked(MatchField.IPV4_SRC)) {
                    Masked<IPv4Address> maskedIp = match.getMasked(MatchField.IPV4_SRC);
                    ip4Prefix = Ip4Prefix.valueOf(
                            maskedIp.getValue().getInt(),
                            maskedIp.getMask().asCidrMaskLength());
                } else {
                    ip4Prefix = Ip4Prefix.valueOf(
                            match.get(MatchField.IPV4_SRC).getInt(),
                            Ip4Prefix.MAX_MASK_LENGTH);
                }
                builder.matchIPSrc(ip4Prefix);
                break;
            case IPV4_DST:
                if (match.isPartiallyMasked(MatchField.IPV4_DST)) {
                    Masked<IPv4Address> maskedIp = match.getMasked(MatchField.IPV4_DST);
                    ip4Prefix = Ip4Prefix.valueOf(
                            maskedIp.getValue().getInt(),
                            maskedIp.getMask().asCidrMaskLength());
                } else {
                    ip4Prefix = Ip4Prefix.valueOf(
                            match.get(MatchField.IPV4_DST).getInt(),
                            Ip4Prefix.MAX_MASK_LENGTH);
                }
                builder.matchIPDst(ip4Prefix);
                break;
            case TCP_SRC:
                builder.matchTcpSrc((short) match.get(MatchField.TCP_SRC).getPort());
                break;
            case TCP_DST:
                builder.matchTcpDst((short) match.get(MatchField.TCP_DST).getPort());
                break;
            case UDP_SRC:
                builder.matchUdpSrc((short) match.get(MatchField.UDP_SRC).getPort());
                break;
            case UDP_DST:
                builder.matchUdpDst((short) match.get(MatchField.UDP_DST).getPort());
                break;
            case MPLS_LABEL:
                builder.matchMplsLabel(MplsLabel.mplsLabel((int) match.get(MatchField.MPLS_LABEL)
                                            .getValue()));
                break;
            case SCTP_SRC:
                builder.matchSctpSrc((short) match.get(MatchField.SCTP_SRC).getPort());
                break;
            case SCTP_DST:
                builder.matchSctpDst((short) match.get(MatchField.SCTP_DST).getPort());
                break;
            case ICMPV4_TYPE:
                byte icmpType = (byte) match.get(MatchField.ICMPV4_TYPE).getType();
                builder.matchIcmpType(icmpType);
                break;
            case ICMPV4_CODE:
                byte icmpCode = (byte) match.get(MatchField.ICMPV4_CODE).getCode();
                builder.matchIcmpCode(icmpCode);
                break;
            case ARP_OP:
            case ARP_SHA:
            case ARP_SPA:
            case ARP_THA:
            case ARP_TPA:
            case MPLS_TC:
            case TUNNEL_ID:
            default:
            }
        }
        return builder.build();
    }
}
