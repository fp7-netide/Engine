package eu.netide.core.api;

/**
 * Created by timvi on 07.08.2015.
 */
public interface IBackendConnector {
    boolean SendData(byte[] data, String backendId);

    void RegisterListener(IConnectorListener listener);
}
