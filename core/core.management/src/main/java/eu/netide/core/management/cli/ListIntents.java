package eu.netide.core.management.cli;

import eu.netide.core.api.IFIBManager;
import eu.netide.core.api.IFlowModEntry;
import eu.netide.core.api.IIntent;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by msp on 11/12/16.
 */
@Command(scope = "netide", name = "listIntents", description = "List identified intents")
public class ListIntents extends OsgiCommandSupport {
    @Option(name = "-v", aliases = {"--verbose"}, required = false, multiValued = false,
            description = "List FlowMods associated with intents as well")
    private boolean verbose = false;

    @Override
    protected Object doExecute() throws Exception {
        IFIBManager fibManager = getService(IFIBManager.class);

        Map<Integer, Set<IIntent>> moduleIdToIntent = new HashMap<>();
        for (IIntent intent: fibManager.getIntents()) {
            int id = intent.getModuleId();
            if (! moduleIdToIntent.containsKey(id)) {
                moduleIdToIntent.put(id, new HashSet<>());
            }

            moduleIdToIntent.get(id).add(intent);
        }

        for (Integer key : moduleIdToIntent.keySet()) {
            System.out.format("ModuleID: %d\n", key);
            for (IIntent intent: moduleIdToIntent.get(key)) {
                System.out.format("\t%s\n", intent.toString());
                if (! verbose) {
                    continue;
                }

                for (IFlowModEntry flowModEntry: intent.getFlowModEntries()) {
                    System.out.format("\t\t%s\n", flowModEntry);
                }
            }
        }

        return null;
    }
}