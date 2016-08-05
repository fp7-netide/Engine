package eu.netide.core.api;

import org.onosproject.net.flow.FlowEntry;

import java.util.List;

/**
 * Created by msp on 1/13/16.
 */
public interface IFIBManager {

    /**
     * Returns a list of installed FlowMods.
     *
     * @return The list of FlowMods.
     */
    List<FlowEntry> getFlowMods();
}
