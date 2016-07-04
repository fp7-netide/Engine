package eu.netide.core.api;

import eu.netide.lib.netip.Message;

import java.util.Collection;

/**
 * Created by arne on 28.06.16.
 */
public interface IOFRoutingManager {

    void sendRequest(Message m, int moduleId);

    public Collection<? extends OFRoutingRequest> getRoutingRequestStatus();


}
