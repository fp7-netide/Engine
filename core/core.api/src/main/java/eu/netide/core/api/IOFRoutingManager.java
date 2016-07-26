package eu.netide.core.api;

import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.OpenFlowMessage;

import java.util.Collection;

/**
 * Created by arne on 28.06.16.
 */
public interface IOFRoutingManager {

    public Collection<? extends OFRoutingRequest> getRoutingRequestStatus();


    /**
     * Sends a new request, where answers are only send to the requested destionation
     *
     * @param m        the OpenFlowMessage
     * @param moduleId The module/backend that the request should be delivered
     *                 to. Can be set to null if the result should be only be
     *                 delivered to the callback function.
     * @param callback method call on delivery of new message, can be empty when
     *                 the result is only indented to be send back to module
     *                 in question
     */
    void sendRequest(OpenFlowMessage m, String moduleId, RoutingRequestCallback callback);
}
