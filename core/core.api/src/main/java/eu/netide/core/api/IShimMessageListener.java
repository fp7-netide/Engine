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
     */
    void OnShimMessage(Message message);
}
