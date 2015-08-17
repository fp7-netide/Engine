package eu.netide.core.api;

import eu.netide.lib.netip.Message;

/**
 * Created by timvi on 07.08.2015.
 */
public interface IBackendManager {
    boolean sendMessage(Message message, String backendId);

    Iterable<String> getBackendIds();
}
