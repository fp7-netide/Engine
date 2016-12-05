package eu.netide.core.api;

import java.util.Set;

/**
 * Created by msp on 11/26/16.
 */
public interface IIntent {
    /**
     * Returns the moduleId of the module that issued the intent.
     * @return ModuleId.
     */
    int getModuleId();

    /**
     * Returns all FlowModEntries associated with the intent.
     * @return Set of associated FlowMods.
     */
    Set<IFlowModEntry> getFlowModEntries();

    /**
     * Adds a FlowModEntry to the intent.
     * @param flowMod FlowModEntry to add.
     */
    void addFlowModEntry(IFlowModEntry flowMod);
}
