package eu.netide.core.globalfib;

import eu.netide.core.topology.ITopology;
import eu.netide.core.topology.Switch;
import eu.netide.core.topology.SwitchPort;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

/**
 * Created by arne on 25.08.15.
 */

public class GlobalFIB {
    private static final MatchField[] fieldList = new MatchField[]{
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
            MatchField.UDP_DST
    };

    static class FIBMatch {

        //!
    }

    static class FIBEntry {

        //! Every packet which matches this match is mapped to this rule
        Match match;

        //! Set of DPIDs for which the Match is valid. Empty list means all switches
        //! Only edge ports by default
        Set<Long> dpids;

        //! This is the cummulative action of FlowMod that are put into this entry
        OFAction action;
    }


    LinkedList<FIBEntry> globalfib = new LinkedList<>();


    HashMap<Long, Vector<OFFlowMod>> individualSwitchFlowMap = new HashMap<>();

    private ITopology topogloy;

    public void addFlow(long datapathId, OFFlowMod ofFlowAdd) {
        if (!individualSwitchFlowMap.containsKey(datapathId)) {
            individualSwitchFlowMap.put(datapathId, new Vector<>());
        }



    }

    public boolean handlePacketIn(OFPacketIn packetIn, long datapathId) {
        if (!(packetIn.getVersion().getWireVersion() >= OFVersion.OF_13.wireVersion))
            throw new RuntimeException("Only 1.3+ supported at the moment");

        // Check if switch is in toplogy
        Switch sw = topogloy.getSwitch(datapathId);
        if (sw == null){
            // XXX: Toplogy not ready. FIXME: What to do here.
            return false;
        }

        // Decode Ethernet packet
        Ethernet ethernet = new Ethernet();
        IPacket ippacket = ethernet.deserialize(packetIn.getData(), 0, packetIn.getData().length);

        OFPort inport = packetIn.getMatch().get(MatchField.IN_PORT);
        SwitchPort swInport = sw.getPort(inport);

        // If the inport is an external Port just check if we have an entry in the Global FIB
        if (swInport.edgePort) {
            getGlobalFlow(swInport, packetIn.getMatch());
        }


        return false;
    }

    /* Looks in the GlobalFIB for an Entry that matches the specifics */
    private void getGlobalFlow(SwitchPort inport, Match match) {
        // Iterate through all flows in the global FIB and see if an entry matches
        for (FIBEntry fibEntry : globalfib) {
            for (MatchField<?> field:fibEntry.match.getMatchFields())
            {
            }

        }
    }

}
