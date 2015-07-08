package eu.netide.core.api;

/**
 * Created by timvi on 25.06.2015.
 */
public interface IShimConnector {
    void Open(int port);

    void Close();

    void SendMessage(String message);
}
