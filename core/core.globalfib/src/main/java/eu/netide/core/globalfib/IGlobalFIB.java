package eu.netide.core.globalfib;

import eu.netide.core.globalfib.intent.FlowModEntry;
import eu.netide.core.globalfib.intent.Intent;
import eu.netide.core.globalfib.topology.TopologySpecification;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;

import java.util.Set;

/**
 * Created by msp on 7/7/16.
 */
public interface IGlobalFIB {
    boolean handlePacketIn(OFPacketIn packetIn, long datapathId);

    void addFlowMod(OpenFlowMessage ofMessage);

    Set<FlowModEntry> getFlowModEntries();

    Set<Intent> getIntents();

    void setTopologySpecification(TopologySpecification topologySpecification);
}
