package eu.netide.core.topology;

/**
 * Created by arne on 01.09.15.
 */
public interface ITopology {

    Switch getSwitch(long dpid);
}
