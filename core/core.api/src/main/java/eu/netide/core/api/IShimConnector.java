package eu.netide.core.api;

/**
 * Created by timvi on 25.06.2015.
 */
public interface IShimConnector {

    boolean SendData(byte[] data);

    void RegisterListener(IConnectorListener listener);
}
