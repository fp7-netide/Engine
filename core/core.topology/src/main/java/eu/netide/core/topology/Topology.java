package eu.netide.core.topology;

import java.util.HashMap;

/**
 * Created by arne on 27.08.15.
 */
public class Topology implements ITopology {

    //! HashMap from DPID to Switch Object
    HashMap<Long, Switch> switches = new HashMap<>();


    @Override
    public Switch getSwitch(long dpid) {
        return switches.get(dpid);
    }
}
