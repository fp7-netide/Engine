package eu.netide.core.api;

import eu.netide.lib.netip.Message;

/**
 * Created by msp on 1/13/16.
 */
public interface IFIBManager {
    /**
     * Handles the given message (given by caos).
     *
     * @param message The message to be handled.
     */
    void handleMessage(Message message);
}
