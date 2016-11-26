package eu.netide.core.api;

import org.projectfloodlight.openflow.protocol.OFFlowMod;

/**
 * Created by msp on 11/26/16.
 */
public interface IFlowModEntry {
    /**
     * Returns the FlowMod.
     * @return FlowMod.
     */
    OFFlowMod getFlowMod();

    /**
     * Returns the dpid of the device the applies to FlowMod.
     * @return Dpid.
     */
    long getDpid();

    /**
     * Returns the moduleId of the module that issued the FlowMod
     * @return ModuleId.
     */
    int getModuleId();
}
