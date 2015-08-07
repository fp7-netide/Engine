package eu.netide.core.api;

import eu.netide.core.api.netip.Message;

/**
 * Created by timvi on 07.08.2015.
 */
public interface IBackendMessageListener {
    void OnBackendMessage(Message message, String originId);
}
