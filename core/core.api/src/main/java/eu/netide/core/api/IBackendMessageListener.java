package eu.netide.core.api;

import eu.netide.lib.netip.Message;

import java.util.LinkedList;

/**
 * Interface for backend message listeners.
 *
 * Created by timvi on 07.08.2015.
 */
public interface IBackendMessageListener {
    /**
     * Called by the manager when a message from a backend was received.
     *
     * @return If the message was process or not. Currently used to pass through unconsumed message
     * @param message  The received message.
     * @param originId The id of the backend that sent the message.
     *
     */
    MessageHandlingResult OnBackendMessage(Message message, String originId);

    /**
     * Called *after* a backend has been removed
     * @param backEndName the backend that has been removed
     * @param removedModules
     */
    void OnBackendRemoved(String backEndName, LinkedList<Integer> removedModules);

    /**
     * Provides a copy of every message sent to a backend.
     * @param message The message sent.
     * @param backendId Backend id the message was sent to.
     */
    void OnOutgoingBackendMessage(Message message, String backendId);
}
