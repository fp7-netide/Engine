package eu.netide.core.api;

import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.OpenFlowMessage;

import java.util.Collection;

/**
 * Created by arne on 28.06.16.
 */
public interface IOFRoutingManager {

    public Collection<? extends OFRoutingRequest> getRoutingRequestStatus();


    void sendRequest(OpenFlowMessage m, String moduleId);
}
