package eu.netide.core.caos.composition;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by timvi on 31.08.2015.
 */
public enum EthType {
    UNDEFINED(0x0000), //special empty type
    IPv4(0x0800), // Internet Protocol version 4 (IPv4)
    ARP(0x0806), // Address Resolution Protocol (ARP)
    WAKE_ON_LAN(0x0842), // Wake-on-LAN[3]
    TRILL(0x22F3), // IETF TRILL Protocol
    DECNET_IV(0x6003), // DECnet Phase IV
    REV_ARP(0x8035), // Reverse Address Resolution Protocol
    APPLE_TALK(0x809B), // AppleTalk (Ethertalk)
    APPLE_TALK_ARP(0x80F3), // AppleTalk Address Resolution Protocol (AARP)
    VLAN_FRAME(0x8100), // VLAN-tagged frame (IEEE 802.1Q) & Shortest Path Bridging IEEE 802.1aq[4]
    IPX_8137(0x8137), // IPX
    IPX_8138(0x8138), // IPX
    QNX(0x8204), // QNX Qnet
    IPv6(0x86DD), // Internet Protocol Version 6 (IPv6)
    ETH_FLOW(0x8808), // Ethernet flow control
    SLOW_PROTOCOLS(0x8809), // Slow Protocols (IEEE 802.3)
    COBRANET(0x8819), // CobraNet
    MPLS_UNICAST(0x8847), // MPLS unicast
    MPLS_MULTICAST(0x8848), // MPLS multicast
    PPPoE_DISCOVERY(0x8863), // PPPoE Discovery Stage
    PPPoE_SESSION(0x8864), // PPPoE Session Stage
    JUMBO_FRAMES(0x8870), // Jumbo Frames
    HOMEPLUG_10(0x887B), // HomePlug 1.0 MME
    EAP_OVER_LAN(0x888E), // EAP over LAN (IEEE 802.1X)
    PROFINET(0x8892), // PROFINET Protocol
    HYPERSCSI(0x889A), // HyperSCSI (SCSI over Ethernet)
    ATA_OVER_ETH(0x88A2), // ATA over Ethernet
    ETHERCAT(0x88A4), // EtherCAT Protocol
    BRIDGING(0x88A8), // Provider Bridging (IEEE 802.1ad) & Shortest Path Bridging IEEE 802.1aq[5]
    POWERLINK(0x88AB), // Ethernet Powerlink[citation needed]
    LLDP(0x88CC), // Link Layer Discovery Protocol (LLDP)
    SERCOS(0x88CD), // SERCOS III
    HOMEPLUG_AV(0x88E1), // HomePlug AV MME[citation needed]
    MRP(0x88E3), // Media Redundancy Protocol (IEC62439-2)
    MAC_SEC(0x88E5), // MAC security (IEEE 802.1AE)
    PTP(0x88F7), // Precision Time Protocol (IEEE 1588)
    CFM(0x8902), // IEEE 802.1ag Connectivity Fault Management (CFM) Protocol / ITU-T Recommendation Y.1731 (OAM)
    FCoE(0x8906), // Fibre Channel over Ethernet (FCoE)
    FCoE_INIT(0x8914), // FCoE Initialization Protocol
    RoCE(0x8915), // RDMA over Converged Ethernet (RoCE)
    HSR(0x892F), // High-availability Seamless Redundancy (HSR)
    CONF_TEST(0x9000), // Ethernet Configuration Testing Protocol[6]
    Q_IN_Q(0x9100), // Q-in-Q
    LLT(0xCAFE); // Veritas Low Latency Transport (LLT)[7] for Veritas Cluster Server

    private int value;

    EthType(int value) {
        this.value = value;
    }

    public static EthType fromValue(int intValue) {
        Optional<EthType> ethType = Arrays.stream(values()).filter(e -> e.value == intValue).findFirst();
        if (!ethType.isPresent()) throw new IllegalArgumentException("No EthType for value '" + intValue + "'.");
        return ethType.get();
    }

    public int getValue() {
        return this.value;
    }

    public static EthType fromString(String name) {
        Optional<EthType> ethType = Arrays.stream(values()).filter(e -> e.name().equals(name)).findFirst();
        if (!ethType.isPresent()) throw new IllegalArgumentException("No EthType for name '" + name + "'.");
        return ethType.get();
    }

    public org.projectfloodlight.openflow.types.EthType toOFJEthType() {
        return org.projectfloodlight.openflow.types.EthType.of(this.value);
    }
}
