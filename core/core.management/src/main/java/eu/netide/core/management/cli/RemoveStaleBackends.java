package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by arne on 04.02.16.
 */

@Command(scope = "netide", name = "removeStaleModules", description = "removes all backends not been active")
public class RemoveStaleBackends extends OsgiCommandSupport {
    @Argument(index = 0, name = "timeout", description = "the timeout to use in seconds", required = false, multiValued = false, valueToShowInHelp = "60")
    int timeout=60;

    @Override
    protected Object doExecute() throws Exception {
        IBackendManager backendManager = getService(IBackendManager.class);

        Queue beToRemove = new LinkedList();

        checkBackends(backendManager);


        return null;
    }

    private void checkBackends(IBackendManager backendManager) {
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
                    // Backend remove list modified, restart ...
                    checkBackends(backendManager);
                    return;
                }
            }
        });
    }

}
