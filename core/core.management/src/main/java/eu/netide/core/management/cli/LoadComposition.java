package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.management.ManagementHandler;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by arne on 04.02.16.
 */

@Command(scope = "netide", name = "loadComposition", description = "Load a composition XML file")
public class LoadComposition extends OsgiCommandSupport {

    @Argument(index = 0, name = "compositionFile", description = "The composition xml file to load", required = true, multiValued = false)
    String compositionFile;

    @Override
    protected Object doExecute() throws Exception {
        Path path = Paths.get(compositionFile).toAbsolutePath();
        String spec = new String(Files.readAllBytes(path));

        ManagementHandler.setConfigurationValue("eu.netide.core.caos", "compositionSpecification", spec);
        return null;
    }

}
