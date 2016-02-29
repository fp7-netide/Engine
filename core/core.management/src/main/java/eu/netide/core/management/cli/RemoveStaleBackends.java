package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * Created by arne on 04.02.16.
 */

@Command(scope = "netide", name = "removeStaleModules", description = "removes all backends not been active for 60s")
public class RemoveStaleBackends extends OsgiCommandSupport {
    @Argument(index = 0, name = "timeout", description = "the timeout to use in seconds", required = false, multiValued = false)
    int timeout;

    @Override
    protected Object doExecute() throws Exception {
        IBackendManager backendManager = getService(IBackendManager.class);


        backendManager.getModuleIds().forEach(i -> {
            String backendname = backendManager.getBackend(i);
            String moduleName = backendManager.getModuleName(i);
            String lastMessage;

            long now = System.currentTimeMillis();

            if (backendname.equals(moduleName)) {
                // Backends have the same name as their modulename
                long lastActive = now - backendManager.getLastMessageTime(i);
                if ((lastActive / 1000) > timeout) {
                    System.out.printf("Removing statle backend '%s' (last active %.1fs ago", backendname, lastActive / 1000f);
                    backendManager.removeBackend(i);
                }
            }
        });
        return null;
    }

}
