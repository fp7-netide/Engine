package eu.netide.core.globalfib;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.TCP;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpProto;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4Dst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4Src;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.protocol.ver13.OFFactoryVer13;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanPcp;

/**
 * Created by arne on 09.09.15.
 */
public class PacketMatcher {

    Match constructMatchesFromPacket(IPacket pkt) {
        Ethernet eth = (Ethernet) pkt;

        OFOxmList.Builder oxmListBuilder = new OFOxmList.Builder();

        IPv4 ipv4 = null;
        IPv6 ipv6 = null;

        if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
            ipv4 = (IPv4) eth.getPayload();
        } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
            ipv6 = (IPv6) eth.getPayload();
        }

        OFOxms oxms = OFFactoryVer13.INSTANCE.oxms();


        oxmListBuilder.set(oxms.buildVlanVid().setValue(OFVlanVidMatch.ofRawVid(eth.getVlanID())).build());
        oxmListBuilder.set(oxms.buildVlanPcp().setValue(VlanPcp.of(eth.getPriorityCode())).build());
        oxmListBuilder.set(oxms.buildEthSrc().setValue(MacAddress.of(eth.getSourceMACAddress())).build());
        oxmListBuilder.set(oxms.buildEthDst().setValue(MacAddress.of(eth.getDestinationMACAddress())).build());
        oxmListBuilder.set(oxms.buildEthType().setValue(EthType.of(eth.getEtherType())).build());


        if (ipv4 != null) {

            oxmListBuilder.set(oxms.buildIpProto().setValue(IpProtocol.of(ipv4.getProtocol())).build());
            // Left out: version
            oxmListBuilder.set(oxms.buildIpDscp().setValue(IpDscp.of(ipv4.getDscp())).build());
            // Left out: TTL
            oxmListBuilder.set(oxms.buildIpv4Src().setValue(IPv4Address.of(ipv4.getSourceAddress())).build());
            oxmListBuilder.set(oxms.buildIpv4Dst().setValue(IPv4Address.of(ipv4.getDestinationAddress())).build());

            if (ipv4.getPayload() instanceof TCP) {
                TCP tcp = (TCP) ipv4.getPayload();
                oxmListBuilder.set(oxms.buildTcpSrc().setValue(TransportPort.of(tcp.getSourcePort())).build());
                oxmListBuilder.set(oxms.buildTcpDst().setValue(TransportPort.of(tcp.getDestinationPort())).build());
            }

        } else if (ipv6 != null) {
            /* TODO: IPv6 is todo for later */
            //OFOxmIpProto ipProto = oxms.buildIpProto().setValue(IpProtocol.of(ipv6.get())).build();

        }

        return OFFactoryVer13.INSTANCE.matchV3(oxmListBuilder.build());
    }


    boolean match(IPacket pkt, Match matches) {

/*
        matches.get()
        for (MatchField<?> m : matches.getMatchFields()) {
            boolean doesMatch = false;

            OFValueType<?> mt = matches.get(m);


            if (mt instanceof OFVlanVidMatch) {
                doesMatch = ((OFVlanVidMatch) mt).getRawVid() == eth.getVlanID();
            } else if (mt instanceof)



                switch (m.id) {
                    case VLAN_VID:
                }
            }

        }
*/
        return false;
    }
}
