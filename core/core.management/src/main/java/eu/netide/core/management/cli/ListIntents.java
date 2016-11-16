package eu.netide.core.management.cli;

import eu.netide.core.api.IFIBManager;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * Created by msp on 11/12/16.
 */
@Command(scope = "netide", name = "listIntents", description = "List identified intents")
public class ListIntents extends OsgiCommandSupport {
    @Override
    protected Object doExecute() throws Exception {
        IFIBManager fibManager = getService(IFIBManager.class);

        for (String intentString : fibManager.getIntentStrings()) {
            System.out.println(intentString);
        }

        return null;
    }
}