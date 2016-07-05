package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IOFRoutingManager;
import eu.netide.core.api.IShimManager;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * Created by arne on 04.02.16.
 */

@Command(scope = "netide", name = "listRoutingRequests", description = "List routing requests")
public class ListRoutingRequests extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        IBackendManager backendManager = getService(IBackendManager.class);
        IShimManager shimManager = getService(IShimManager.class);
        IOFRoutingManager routingManager = getService(IOFRoutingManager.class);

        System.out.format("%5s %20s %5s %6s %10s %20s\n", "Shim", "Backend", "BXid", "#Resp", "Type", "Last message (s ago)");
        long now = System.currentTimeMillis();
        routingManager.getRoutingRequestStatus().forEach(r -> {

                                                             String backendname = r.getBackendID();

                                                             String lastMessage = String.format("%.2f", (now - r.getLastTimeActive()) / 1000f);

                                                             System.out.format("%5d %20s %5s %6d %10s %20s\n", r.getShimXid(), backendname, r.getBackendID(),
                                                                               r.getResponses(), r.getReqTypeString(), lastMessage);

                                                         }
        );
        return null;
    }

}
