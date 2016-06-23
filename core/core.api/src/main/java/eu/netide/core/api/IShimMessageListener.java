package eu.netide.core.api;

import eu.netide.lib.netip.Message;

/**
 * Interface for shim message listeners.
 *
 * Created by timvi on 08.07.2015.
 */
public interface IShimMessageListener {
    /**
     * Called by the manager when a message from the shim is received.
     *
     * @param message The received message.
     * @param originId The id of the shim that sent the message.
     */
    void OnShimMessage(Message message, String originId);


    /**
     * Called by the manager when the manager sends a message to the shim.
     * Used for debugging/logging.
     * @param message The message sent.
     */
    void OnOutgoingShimMessage(Message message);
}
