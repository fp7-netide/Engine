package eu.netide.core.globalfib.intent;

import eu.netide.core.api.IFlowModEntry;
import org.onosproject.net.DeviceId;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFFlowMod;

/**
 * Created by msp on 9/28/16.
 */
public class FlowModEntry implements IFlowModEntry {
    private OFFlowMod flowMod;

    private long dpid;

    private int moduleId;

    public FlowModEntry(OFFlowMod flowMod, long dpid, int moduleId) {
        this.flowMod = flowMod;
        this.dpid = dpid;
        this.moduleId = moduleId;
    }

    @Override
    public int getModuleId() {
        return moduleId;
    }

    @Override
    public long getDpid() {
        return dpid;
    }

    @Override
    public OFFlowMod getFlowMod() {
        return flowMod;
    }

    @Override
    public int hashCode() {
        return (int) (moduleId * dpid * flowMod.getMatch().hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlowModEntry) {
            FlowModEntry other = (FlowModEntry) obj;
            if (other.dpid != dpid || other.moduleId != moduleId) {
                return false;
            }
            if (other.flowMod.getMatch().equals(flowMod.getMatch())
                    && other.flowMod.getActions().equals(flowMod.getActions())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String representation = "Dpid=";
        representation += DeviceId.deviceId(Dpid.uri(getDpid()));
        representation += ", [";
        representation += getFlowMod().getType() + ", ";
        representation += "xid=" + getFlowMod().getXid() + ", ";
        representation += getFlowMod().getMatch() + ", ";
        representation += getFlowMod().getActions();
        representation += "]";
        return representation;
    }
}
