package eu.netide.core.api;

import eu.netide.lib.netip.Message;

/**
 * Interface for backend message listeners.
 *
 * Created by timvi on 07.08.2015.
 */
public interface IBackendMessageListener {
    /**
     * Called by the manager when a message from a backend was received.
     *
     * @param message  The received message.
     * @param originId The id of the backend that sent the message.
     */
    void OnBackendMessage(Message message, String originId);
}
