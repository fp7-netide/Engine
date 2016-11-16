package eu.netide.core.api;

import org.projectfloodlight.openflow.protocol.OFFlowMod;

import java.util.Set;

/**
 * Created by msp on 1/13/16.
 */
public interface IFIBManager {

    /**
     * Returns a string representation of all installed FlowMods.
     *
     * @return Set of Strings representing FlowMods.
     */
    Set<String> getFlowModStrings();

    /**
     * Returns a string representation of all every intent.
     *
     * @return Set of Strings representing intents.
     */
    Set<String> getIntentStrings();
}
