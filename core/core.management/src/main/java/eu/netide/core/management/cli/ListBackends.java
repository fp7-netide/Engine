package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import jline.console.completer.FileNameCompleter;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.util.function.Consumer;

/**
 * Created by arne on 04.02.16.
 */

@Command(scope = "netide", name = "listModules", description = "Says hello")
public class ListBackends extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        IBackendManager backendManager = getService(IBackendManager.class);

        System.out.format("%5s %20s %20s\n", "Id", "Name", "Backend");
        backendManager.getModuleIds().forEach(i -> {
                    String backendname = backendManager.getBackend(i);
                    String moduleName = backendManager.getModuleName(i);
            System.out.format("%5d %20s %20s\n", i, moduleName, backendname);

                }

        );
        return null;
    }

}
