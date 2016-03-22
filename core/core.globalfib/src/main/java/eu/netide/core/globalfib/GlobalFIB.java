package eu.netide.core.globalfib;

import eu.netide.lib.netip.OpenFlowMessage;
import org.onosproject.net.flow.*;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.provider.of.flow.impl.*;
import org.projectfloodlight.openflow.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by arne on 25.08.15.
 */

public class GlobalFIB {
    private static final Logger logger = LoggerFactory.getLogger(GlobalFIB.class);

    private List<FlowEntry> globalFIB = new LinkedList<>();

    private HashMap<Long, Vector<OFFlowMod>> individualSwitchFlowMap = new HashMap<>();

    public void addFlowMod(OpenFlowMessage ofMessage) {
        /*
        // The FlowEntryBuilder currently does only support OF version >= 1.3
        if (ofMessage.getOfMessage().getVersion().getWireVersion() < OFVersion.OF_13.getWireVersion()) {
            throw new RuntimeException("Only OpenFlow 1.3+ supported at the moment");
        }

        OFFlowMod flowMod = (OFFlowMod) ofMessage.getOfMessage();
        FlowEntryBuilder flowEntryBuilder = new FlowEntryBuilder(new Dpid(ofMessage.getHeader().getDatapathId()), flowMod, null);
        FlowEntry flowEntry = flowEntryBuilder.build();

        // TODO: correct treatment, if entry already exists?
        if (getFlowEntry(flowEntry) == null) {
            logger.debug("Adding FlowEntry ");
            globalFIB.add(flowEntry);
        }
        */
    }

    public boolean handlePacketIn(OFPacketIn packetIn, long datapathId) {
        if (!(packetIn.getVersion().getWireVersion() >= OFVersion.OF_13.wireVersion))
            throw new RuntimeException("Only 1.3+ supported at the moment");

        return false;
    }

    /**
     * Looks in the GlobalFIB for an Entry that matches the specifics
     */
    private FlowEntry getFlowEntry(FlowEntry flowEntry) {
        for (FlowEntry fibEntry : globalFIB) {
            if (flowEntry.exactMatch(fibEntry)) {
                logger.debug("Found matching flow entry");
                return fibEntry;
            }
        }

        return null;
    }

}
