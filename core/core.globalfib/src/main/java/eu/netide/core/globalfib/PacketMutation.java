package eu.netide.core.globalfib;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwTos;
import org.projectfloodlight.openflow.protocol.action.OFActionSetTpDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetTpSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4Dst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4Src;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv6Dst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv6Src;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmTcpDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmTcpSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmUdpDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmUdpSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanPcp;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by arne on 16.09.15.
 */
public class PacketMutation {
    /* XXX: Fix wildcard/masks */

    private static final Logger logger = LoggerFactory.getLogger(PacketMutation.class);


    Ethernet applyAction(Ethernet eth, List<OFAction> actionList) {
        eth = (Ethernet) eth.clone();
        for (OFAction action : actionList) {
            IPv4 ip = Utils.getIPv4FromEth(eth);
            IPv6 ipv6 = Utils.getProtoFromEth(eth, IPv6.class);
            TCP tcp = Utils.getProtoFromEth(eth, TCP.class);
            UDP udp = Utils.getProtoFromEth(eth, UDP.class);


            switch (action.getType()) {
                default:
                    logger.error("OFM", "Unknown Action Type " + action.getType());
                    break;
                case PUSH_VLAN:
                    /* XXX, really right?! */
                    Ethernet newOuterEth = new Ethernet();
                    newOuterEth.setPayload(newOuterEth);
                    eth = newOuterEth;

                    break;
                case POP_VLAN:
                    if (eth.getPayload() instanceof Ethernet) {
                        eth = (Ethernet) eth.getPayload();
                    }
                    break;
                case SET_DL_DST:
                    eth.setDestinationMACAddress(Utils.OF4jMacToPMac(((OFActionSetDlDst) action).getDlAddr()));
                    break;
                case SET_DL_SRC:
                    eth.setSourceMACAddress(Utils.OF4jMacToPMac(((OFActionSetDlSrc) action).getDlAddr()));
                    break;
                case OUTPUT:
                    /* XXX lookup current switch, and lookup inport of next switch*/
                    break;
                case SET_NW_DST:
                    if (ip == null) {
                        break;
                    }
                    ip.setDestinationAddress(((OFActionSetNwDst) action).getNwAddr().getInt());
                    break;
                case SET_NW_SRC:
                    if (ip == null) {
                        break;
                    }
                    ip.setSourceAddress(((OFActionSetNwSrc) action).getNwAddr().getInt());
                    break;
                case SET_TP_DST:
                    if (tcp != null) {
                        tcp.setDestinationPort((short) ((OFActionSetTpDst) action).getTpPort().getPort());
                    }
                    if (udp != null) {
                        udp.setDestinationPort((short) ((OFActionSetTpDst) action).getTpPort().getPort());
                    }
                    break;
                case SET_TP_SRC:
                    tcp = Utils.getProtoFromEth(eth, TCP.class);
                    udp = Utils.getProtoFromEth(eth, UDP.class);
                    if (tcp != null) {
                        tcp.setSourcePort((short) ((OFActionSetTpSrc) action).getTpPort().getPort());
                    }
                    if (udp != null) {
                        udp.setSourcePort((short) ((OFActionSetTpSrc) action).getTpPort().getPort());
                    }
                    break;

                case SET_NW_TOS:
                    ip = Utils.getIPv4FromEth(eth);
                    if (ip != null) {
                        ip.setDscp((byte) ((OFActionSetNwTos) action).getNwTos());
                    }
                    break;

                case SET_FIELD:
                    OFActionSetField ofActionSetField = (OFActionSetField) action;
                    OFOxm<?> field = ofActionSetField.getField();

                    if (field instanceof OFOxmVlanVid) {
                        eth.setVlanID(((OFOxmVlanVid) field).getValue().getVlan());
                    } else if (field instanceof OFOxmVlanPcp) {
                        eth.setPriorityCode(((OFOxmVlanPcp) field).getValue().getValue());
                    } else if (field instanceof OFOxmIpv4Src) {
                        if (ip != null) {
                            ip.setSourceAddress(((OFOxmIpv4Src) field).getValue().getInt());
                        }
                    } else if (field instanceof OFOxmIpv4Dst) {
                        if (ip != null) {
                            ip.setDestinationAddress((((OFOxmIpv4Dst) field).getValue().getInt()));
                        }
                    } else if (field instanceof OFOxmIpv6Dst) {
                        if (ipv6 != null) {
                            ipv6.setDestinationAddress(((OFOxmIpv6Dst) field).getValue().getBytes());
                        }
                    } else if (field instanceof OFOxmIpv6Src) {
                        if (ipv6 != null) {
                            ipv6.setSourceAddress(((OFOxmIpv6Src) field).getValue().getBytes());
                        }
                    } else if (field instanceof OFOxmTcpDst) {
                        if (tcp != null) {
                            tcp.setDestinationPort((short) ((OFOxmTcpDst) field).getValue().getPort());
                        }
                    } else if (field instanceof OFOxmUdpDst) {
                        if (udp != null) {
                            udp.setDestinationPort((short) ((OFOxmUdpDst) field).getValue().getPort());
                        }
                    } else if (field instanceof OFOxmTcpSrc) {
                        if (tcp != null) {
                            tcp.setSourcePort((short) ((OFOxmTcpSrc) field).getValue().getPort());
                        }
                    } else if (field instanceof OFOxmUdpSrc) {
                        if (udp != null) {
                            udp.setSourcePort((short) ((OFOxmUdpSrc) field).getValue().getPort());
                        }
                    } else {
                        logger.error("OFM", "Unknown Action  Field Type " + action.getType() + " " + field.getValue());
                    }


            }
            break;


        }

        return eth;
    }
}
