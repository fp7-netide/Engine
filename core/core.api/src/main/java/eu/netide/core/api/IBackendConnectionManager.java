package eu.netide.core.api;

/**
 * Created by timvi on 25.06.2015.
 */
public interface IBackendConnectionManager {
    // TODO query backend, etc.
    void QueryPacketIn(NetworkApplicationInfo app, PacketInEventArgs args);
}
