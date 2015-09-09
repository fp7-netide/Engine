package eu.netide.core.caos.composition;

import java.util.Arrays;
import java.util.Optional;

/**
 * Re-creation of OpenFlowJ IpProtocol to allow serialization.
 * <p>
 * Created by timvi on 09.09.2015.
 */
public enum IpProtocol {

    HOPOPT(0x00),
    ICMP(0x01),
    IGMP(0x02),
    GGP(0x03),
    IPv4(0x04),
    ST(0x05),
    TCP(0x06),
    CBT(0x07),
    EGP(0x08),
    IGP(0x09),
    BBN_RCC_MON(0x0A),
    NVP_II(0x0B),
    PUP(0x0C),
    ARGUS(0x0D),
    EMCON(0x0E),
    XNET(0x0F),
    CHAOS(0x10),
    UDP(0x11),
    MUX(0x12),
    DCN_MEAS(0x13),
    HMP(0x14),
    PRM(0x15),
    XNS_IDP(0x16),
    TRUNK_1(0x17),
    TRUNK_2(0x18),
    LEAF_1(0x19),
    LEAF_2(0x1A),
    RDP(0x1B),
    IRTP(0x1C),
    ISO_TP4(0x1D),
    NETBLT(0x1E),
    MFE_NSP(0x1F),
    MERIT_INP(0x20),
    DCCP(0x21),
    _3PC(0x22),
    IDPR(0x23),
    XTP(0x24),
    DDP(0x25),
    IDPR_CMTP(0x26),
    TP_PP(0x27),
    IL(0x28),
    IPv6(0x29),
    SDRP(0x2A),
    IPv6_ROUTE(0x2B),
    IPv6_FRAG(0x2C),
    IDRP(0x2D),
    RSVP(0x2E),
    GRE(0x2F),
    MHRP(0x30),
    BNA(0x31),
    ESP(0x32),
    AH(0x33),
    I_NLSP(0x34),
    SWIPE(0x35),
    NARP(0x36),
    MOBILE(0x37),
    TLSP(0x38),
    SKIP(0x39),
    IPv6_ICMP(0x3A),
    IPv6_NO_NXT(0x3B),
    IPv6_OPTS(0x3C),
    HOST_INTERNAL(0x3D),
    CFTP(0x3E),
    LOCAL_NET(0x3F),
    SAT_EXPAK(0x40),
    KRYPTOLAN(0x41),
    RVD(0x42),
    IPPC(0x43),
    DIST_FS(0x44),
    SAT_MON(0x45),
    VISA(0x46),
    IPCV(0x47),
    CPNX(0x48),
    CPHB(0x49),
    WSN(0x4A),
    PVP(0x4B),
    BR_SAT_MON(0x4C),
    SUN_ND(0x4D),
    WB_MON(0x4E),
    WB_EXPAK(0x4F),
    ISO_IP(0x50),
    VMTP(0x51),
    SECURE_VMTP(0x52),
    VINES(0x53),
    TTP_IPTM(0x54),
    NSFNET_IGP(0x55),
    DGP(0x56),
    TCF(0x57),
    EIGRP(0x58),
    OSPF(0x59),
    Sprite_RPC(0x5A),
    LARP(0x5B),
    MTP(0x5C),
    AX_25(0x5D),
    IPIP(0x5E),
    MICP(0x5F),
    SCC_SP(0x60),
    ETHERIP(0x61),
    ENCAP(0x62),
    PRIVATE_ENCRYPT(0x63),
    GMTP(0x64),
    IFMP(0x65),
    PNNI(0x66),
    PIM(0x67),
    ARIS(0x68),
    SCPS(0x69),
    QNX(0x6A),
    A_N(0x6B),
    IP_COMP(0x6C),
    SNP(0x6D),
    COMPAQ_PEER(0x6E),
    IPX_IN_IP(0x6F),
    VRRP(0x70),
    PGM(0x71),
    ZERO_HOP(0x72),
    L2TP(0x73),
    DDX(0x74),
    IATP(0x75),
    STP(0x76),
    SRP(0x77),
    UTI(0x78),
    SMP(0x79),
    SM(0x7A),
    PTP(0x7B),
    IS_IS_OVER_IPv4(0x7C),
    FIRE(0x7D),
    CRTP(0x7E),
    CRUDP(0x7F),
    SSCOPMCE(0x80),
    IPLT(0x81),
    SPS(0x82),
    PIPE(0x83),
    SCTP(0x84),
    FC(0x85),
    RSVP_E2E_IGNORE(0x86),
    MOBILITY_HEADER(0x87),
    UDP_LITE(0x88),
    MPLS_IN_IP(0x89),
    MANET(0x8A),
    HIP(0x8B),
    SHIM6(0x8C);

    private int value;

    IpProtocol(int value) {
        this.value = value;
    }

    public static IpProtocol fromValue(int intValue) {
        Optional<IpProtocol> ipProto = Arrays.stream(values()).filter(e -> e.value == intValue).findFirst();
        if (!ipProto.isPresent()) throw new IllegalArgumentException("No IpProtocol for value '" + intValue + "'.");
        return ipProto.get();
    }

    public int getValue() {
        return this.value;
    }

    public static IpProtocol fromString(String name) {
        Optional<IpProtocol> ipProto = Arrays.stream(values()).filter(e -> e.name().equals(name)).findFirst();
        if (!ipProto.isPresent()) throw new IllegalArgumentException("No IpProtocol for name '" + name + "'.");
        return ipProto.get();
    }

    public org.projectfloodlight.openflow.types.IpProtocol toOFJIpProtocol() {
        return org.projectfloodlight.openflow.types.IpProtocol.of((short) this.value);
    }
}
