package eu.netide.core.api;

/**
 * Created by timvi on 07.08.2015.
 */
public interface IConnectorListener {
    void OnDataReceived(byte[] data, String originId);
}
