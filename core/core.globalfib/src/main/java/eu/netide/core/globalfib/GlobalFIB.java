package eu.netide.core.globalfib;

import eu.netide.core.api.IFlowModEntry;
import eu.netide.core.api.IIntent;
import eu.netide.core.globalfib.intent.FlowModEntry;
import eu.netide.core.globalfib.intent.Intent;
import eu.netide.core.globalfib.intent.IntentService;
import eu.netide.core.globalfib.topology.HostManager;
import eu.netide.core.globalfib.topology.TopologyManager;
import eu.netide.core.globalfib.topology.TopologySpecification;
import eu.netide.lib.netip.OpenFlowMessage;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.projectfloodlight.openflow.protocol.*;

import java.util.*;

/**
 * Created by arne on 25.08.15.
 */
@Component(immediate=true)
@Service
public class GlobalFIB implements IGlobalFIB {
    private Set<IFlowModEntry> flowModEntries = new HashSet<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    private boolean isTopologyInitialized = false;

    @Override
    public void addFlowMod(OpenFlowMessage ofMessage) {
        if (ofMessage.getOfMessage().getType() != OFType.FLOW_MOD) {
            return;
        }
        FlowModEntry flowModEntry = new FlowModEntry(
                (OFFlowMod) ofMessage.getOfMessage(),
                ofMessage.getHeader().getDatapathId(),
                ofMessage.getHeader().getModuleId());
        flowModEntries.add(flowModEntry);

        if (isTopologyInitialized) {
            intentService.process(flowModEntry);
        }
    }

    @Override
    public boolean handlePacketIn(OFPacketIn packetIn, long datapathId) {
        if (!(packetIn.getVersion().getWireVersion() >= OFVersion.OF_13.wireVersion))
            throw new RuntimeException("Only 1.3+ supported at the moment");

        return false;
    }

    @Override
    public Set<IFlowModEntry> getFlowModEntries() {
        return flowModEntries;
    }

    @Override
    public Set<IIntent> getIntents() {
        return intentService.getIntents();
    }

    @Override
    public void setTopologySpecification(TopologySpecification topologySpecification) {
        // TODO: catch exception, if another class implements the interface (e.g. from onos)
        ((TopologyManager) topologyService).setTopologySpecification(topologySpecification);
        ((HostManager) hostService).setTopologySpecification(topologySpecification);
        isTopologyInitialized = true;
    }
}
