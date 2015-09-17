package eu.netide.core.globalfib;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionPushVlan;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwTos;
import org.projectfloodlight.openflow.protocol.action.OFActionSetTpDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetTpSrc;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.EthType;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by arne on 16.09.15.
 */
public class PacketMutation {
    /* XXX: Fix wildcard/masks */


    Ethernet applyInstructions(Ethernet eth, List<OFInstruction> instructions)
    {
        for (OFInstruction inst: instructions) {
            switch(inst.getType()){

            }

        }
    }

    Ethernet applyAction(Ethernet eth, List<OFAction> actionList) {
    for (OFAction action : actionList) {

            switch (action.getType()) {
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
                    IPv4 ip = Utils.getIPv4FromEth(eth);
                    if (ip == null)
                        break;
                    ip.setDestinationAddress(((OFActionSetNwDst) action).getNwAddr().getInt());
                    break;
                case SET_NW_SRC:
                    ip = Utils.getIPv4FromEth(eth);
                    if (ip == null)
                        break;
                    ip.setSourceAddress(((OFActionSetNwSrc) action).getNwAddr().getInt());
                    break;
                case SET_TP_DST:
                    TCP tcp = Utils.getProtoFromEth(eth, TCP.class);
                    UDP udp = Utils.getProtoFromEth(eth, UDP.class);
                    if (tcp !=null)
                        tcp.setDestinationPort((short) ((OFActionSetTpDst) action).getTpPort().getPort());
                    if (udp !=null)
                        udp.setDestinationPort((short) ((OFActionSetTpDst) action).getTpPort().getPort());
                    break;
                case SET_TP_SRC:
                    tcp = Utils.getProtoFromEth(eth, TCP.class);
                    udp = Utils.getProtoFromEth(eth, UDP.class);
                    if (tcp !=null)
                        tcp.setSourcePort((short) ((OFActionSetTpSrc) action).getTpPort().getPort());
                    if (udp !=null)
                        udp.setSourcePort((short) ((OFActionSetTpSrc) action).getTpPort().getPort());
                    break;

                case SET_NW_TOS:
                    ip = Utils.getIPv4FromEth(eth);
                    if (ip != null)
                        ip.setDscp((byte) ((OFActionSetNwTos) action).getNwTos());
                        break;


            }


        }
    }
}
