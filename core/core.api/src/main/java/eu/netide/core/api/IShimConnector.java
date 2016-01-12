package eu.netide.core.api;

/**
 * Interface for shimconnectors.
 *
 * Created by timvi on 25.06.2015.
 */
public interface IShimConnector {

    /**
     * Sends the given byte array to the shim
     *
     * @param data The data to send.
     * @return True, if transmission was successful, false otherwise.
     */
    boolean SendData(byte[] data);

    /**
     * Registers the given listener.
     *
     * @param listener The listener to handle incoming data.
     */
    void RegisterShimListener(IConnectorListener listener);
}
