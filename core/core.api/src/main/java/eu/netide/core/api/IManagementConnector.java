package eu.netide.core.api;

/**
 * Created by timvi on 06.08.2015.
 */
public interface IManagementConnector {
    /**
     * Sends the given byte array
     *
     * @param data   The data to send.
     * @param target The target to send to.
     * @return True, if transmission was successful, false otherwise.
     */
    boolean SendData(byte[] data, String target);

    /**
     * Registers the given management listener.
     *
     * @param listener The  connector listener to handle management data.
     */
    void RegisterManagementListener(IConnectorListener listener);
}
