package eu.netide.core.api;

import org.projectfloodlight.openflow.protocol.OFFlowMod;

import java.util.Set;

/**
 * Created by msp on 1/13/16.
 */
public interface IFIBManager {

    /**
     * Returns a list of installed FlowMods.
     *
     * @return The list of FlowMods.
     */
    Set<OFFlowMod> getFlowMods();

    /**
     * Returns a string representation of all every intent.
     *
     * @return Set of Strings representing intents.
     */
    Set<String> getIntentStrings();
}
