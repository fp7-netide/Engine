package eu.netide.core.management.cli;

import eu.netide.core.api.IFIBManager;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.onosproject.net.flow.FlowEntry;
import org.projectfloodlight.openflow.protocol.OFFlowMod;

import java.util.Iterator;

/**
 * Created by msp on 5/4/16.
 */

@Command(scope = "netide", name = "listFlowMods", description = "List installed FlowMods")
public class ListFlowMods extends OsgiCommandSupport {
    @Override
    protected Object doExecute() throws Exception {
        IFIBManager fibManager = getService(IFIBManager.class);

        for (OFFlowMod flowMod : fibManager.getFlowMods()) {
            System.out.format("%s\n", flowMod.toString());
        }
        return null;
    }
}
