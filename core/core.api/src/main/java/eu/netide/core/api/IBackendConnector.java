package eu.netide.core.api;

/**
 * Interface for backend connectors.
 *
 * Created by timvi on 07.08.2015.
 */
public interface IBackendConnector {
    /**
     * Sends the given byte array to the specified backend.
     *
     * @param data      The data to be sent.
     * @param backendId The id of the target backend.
     * @return True, if transmission was succesful, false otherwise.
     */
    boolean SendData(byte[] data, String backendId);

    /**
     * Registers the given backend listener.
     *
     * @param listener The  connector listener to handle backend data.
     */
    void RegisterBackendListener(IConnectorListener listener);
}
