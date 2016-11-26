package eu.netide.core.globalfib.intent;

import eu.netide.core.api.IFlowModEntry;
import eu.netide.core.api.IIntent;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by msp on 9/14/16.
 */
public abstract class Intent implements IIntent {
    /**
     * moduleId of the module that issued this intent.
     */
    private final int moduleId;

    /**
     * Set of FlowMods used to implement this intent.
     */
    private Set<IFlowModEntry> flowModEntries = new HashSet<>();

    protected Intent(int moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public void addFlowModEntry(IFlowModEntry flowMod) {
        flowModEntries.add(flowMod);
    }

    @Override
    public Set<IFlowModEntry> getFlowModEntries() {
        return flowModEntries;
    }

    @Override
    public int getModuleId() {
        return moduleId;
    }

    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
