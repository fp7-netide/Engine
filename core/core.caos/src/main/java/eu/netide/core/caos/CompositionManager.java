package eu.netide.core.caos;

import eu.netide.core.api.FlowModEventArgs;
import eu.netide.core.api.PacketInEventArgs;

/**
 * Created by timvi on 25.06.2015.
 */
public class CompositionManager implements ICompositionManager {

    public void Initialize() {
        // TODO read specification, startup subapplications
    }

    public void OnPacketIn(PacketInEventArgs args) {
        System.out.println("Packet-In occurred!");
    }

    public void OnFlowMod(FlowModEventArgs args) {
        System.out.println("Flow-Mod occurred!");
    }
}
