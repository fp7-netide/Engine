package eu.netide.core.api;

/**
 * Created by timvi on 25.06.2015.
 *
 * Interface for Event listeners.
 */
public interface ICoreEventListener {
    void OnPacketIn(PacketInEventArgs args);

    void OnFlowMod(FlowModEventArgs args);
}
