package eu.netide.core.globalfib.intent;

import eu.netide.core.globalfib.FlowModEntry;
import org.projectfloodlight.openflow.protocol.OFFlowMod;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by msp on 9/14/16.
 */
public abstract class Intent {
    /**
     * moduleId of the module that issued this intent.
     */
    private final int moduleId;

    /**
     * Set of FlowMods used to implement this intent.
     */
    private Set<FlowModEntry> flowModEntries = new HashSet<>();

    protected Intent() {
        moduleId = -1;
    }

    protected Intent(int moduleId) {
        this.moduleId = moduleId;
    }

    public void addFlowModEntry(FlowModEntry flowMod) {
        flowModEntries.add(flowMod);
    }

    public Set<FlowModEntry> getFlowModEntries() {
        return flowModEntries;
    }

    public int getModuleId() {
        return moduleId;
    }

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);
}
