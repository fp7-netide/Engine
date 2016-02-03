package eu.netide.core.api;

/**
 * Interface for connector listeners.
 *
 * Created by timvi on 07.08.2015.
 */
public interface IConnectorListener {
    /**
     * Called by the connector when data is received.
     *
     * @param data     The data that is received.
     * @param originId The id of the peer that send the data.
     */
    void OnDataReceived(byte[] data, String originId);
}
