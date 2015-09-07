package eu.netide.core.topology;

import org.projectfloodlight.openflow.types.OFPort;

import java.util.HashMap;
import java.util.Vector;

/**
 * Created by arne on 27.08.15.
 */
public class Switch {
    public final long dpid;
    HashMap<Integer, SwitchPort> ports = new HashMap<>();

    public Switch(long dpid) {
        this.dpid = dpid;

    }



    public SwitchPort getPort(OFPort inPort) {
        return ports.get(inPort.getPortNumber());
    }
}
