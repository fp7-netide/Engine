package eu.netide.core.api;

import eu.netide.lib.netip.OpenFlowMessage;

/**
 * Created by arne on 20.07.16.
 */
public interface RoutingRequestCallback {

    void onResponseReceived(OpenFlowMessage message);
}
