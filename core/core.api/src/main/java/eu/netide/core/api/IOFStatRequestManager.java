package eu.netide.core.api;

import eu.netide.lib.netip.Message;

/**
 * Created by arne on 28.06.16.
 */
public interface IOFStatRequestManager {

    void sendAsyncRequest(Message m, int moduleId);
}
