package eu.netide.core.management.cli;

import eu.netide.core.management.ManagementHandler;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created by msp on 7/8/16.
 */
@Command(scope = "netide", name = "loadTopology", description = "Load a topology XML file")
public class LoadTopology extends OsgiCommandSupport {

    @Argument(index = 0, name = "topologyFile", description = "The topology xml file to load", required = true, multiValued = false)
    String topologyFile;

    @Override
    protected Object doExecute() throws Exception {
        Path path = Paths.get(topologyFile).toAbsolutePath();
        String spec = new String(Files.readAllBytes(path));

        ManagementHandler.setConfigurationValue("eu.netide.core.globalfib", "topologySpecification", spec);
        return null;
    }
}
