package eu.netide.core.api;

import eu.netide.lib.netip.Message;

/**
 * Interface for shim managers.
 *
 * Created by timvi on 08.07.2015.
 */
public interface IShimManager {
    /**
     * Gets the connector that is being used.
     *
     * @return The connector.
     */
    IShimConnector getConnector();

    /**
     * Sends the given message to the shim.
     *
     * @param message The message to send.
     * @return True, if the transmission was succesful, false otherwise.
     */
    boolean sendMessage(Message message);
}
