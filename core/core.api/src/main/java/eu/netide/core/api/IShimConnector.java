package eu.netide.core.api;

/**
 * Created by timvi on 25.06.2015.
 */
public interface IShimConnector {
    // TODO send flowmod, etc.
    void Open(int port);

    void Close();
}
