package eu.netide.core.api;

import org.javatuples.Pair;
import org.projectfloodlight.openflow.protocol.OFFlowMod;

import java.util.Map;
import java.util.Set;

/**
 * Created by msp on 1/13/16.
 */
public interface IFIBManager {

    /**
     * Returns all FlowModEntries stored in the GlobalFIB.
     *
     * @return Set of FlowModEntries.
     */
    Set<IFlowModEntry> getFlowModEntries();

    /**
     * Returns all Intents identified by the GlobalFIB.
     *
     * @return Set of Intents.
     */
    Set<IIntent> getIntents();
}
