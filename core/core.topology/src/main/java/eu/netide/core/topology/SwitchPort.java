package eu.netide.core.topology;

/**
 * Created by arne on 27.08.15.
 */
public class SwitchPort {
    public boolean edgePort = true;
    SwitchPort connectedTo;
    int ofPortNumber;
}
