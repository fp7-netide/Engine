package eu.netide.core.management.cli;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IOFRoutingManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.OFRoutingRequest;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import java.util.Collection;
import java.util.Vector;

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

        System.out.format("%8s %23s %6s %8s %8s %-25s\n", "Shim xid", "Backend", "Bck xid", "#Resp", "LastMsg", "Type");
        long now = System.currentTimeMillis();
        Vector<? extends OFRoutingRequest> requestList = new Vector<>(routingManager.getRoutingRequestStatus());

        requestList.stream().sorted((x,y) -> ( Long.compare(x.getLastTimeActive(),y.getLastTimeActive())))
            .forEach(r -> {

                                                             String backendname = r.getBackendID();

                                                             String lastMessage = String.format("%.2f", (now - r.getLastTimeActive()) / 1000f);

                                                             System.out.format("%8d %23s %6d %8d %7ss %-25s\n", r.getShimXid(), backendname,
                                                                               r.getBackendXid(),  r.getResponses(), lastMessage, r.getReqTypeString());

                                                         }
        );
        return null;
    }

}
