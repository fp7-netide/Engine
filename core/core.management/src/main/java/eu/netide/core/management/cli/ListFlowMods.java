package eu.netide.core.management.cli;

import eu.netide.core.api.IFIBManager;
import eu.netide.core.api.IFlowModEntry;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by msp on 5/4/16.
 */

@Command(scope = "netide", name = "listFlowMods", description = "List installed FlowMods")
public class ListFlowMods extends OsgiCommandSupport {
    @Override
    protected Object doExecute() throws Exception {
        IFIBManager fibManager = getService(IFIBManager.class);

        Map<Integer, Set<IFlowModEntry>> moduleIdToFlowModEntry = new HashMap<>();
        for (IFlowModEntry flowModEntry : fibManager.getFlowModEntries()) {
            int id = flowModEntry.getModuleId();
            if (! moduleIdToFlowModEntry.containsKey(id)) {
                moduleIdToFlowModEntry.put(id, new HashSet<>());
            }

            moduleIdToFlowModEntry.get(id).add(flowModEntry);
        }

        for (Integer key : moduleIdToFlowModEntry.keySet()) {
            System.out.format("ModuleID: %d\n", key);
            for (IFlowModEntry flowModEntry : moduleIdToFlowModEntry.get(key)) {
                System.out.format("\t%s\n", flowModEntry.toString());
            }
        }

        return null;
    }
}
