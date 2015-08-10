package eu.netide.core.api;

import eu.netide.lib.netip.Message;

import java.util.List;

/**
 * Created by timvi on 07.08.2015.
 */
public interface IBackendManager {
    boolean sendMessage(Message message, String backendId);

    List<String> getBackendIds();
}
