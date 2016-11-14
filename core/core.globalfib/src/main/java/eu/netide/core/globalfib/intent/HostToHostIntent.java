package eu.netide.core.globalfib.intent;

import org.onlab.packet.MacAddress;
import org.onosproject.net.Host;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;

/**
 * Created by msp on 9/14/16.
 */
public class HostToHostIntent extends Intent {

    private Host source;

    private Host destination;

    public HostToHostIntent(int moduleId, Host source, Host destination) {
        super(moduleId);
        this.source = source;
        this.destination = destination;
    }

    public Host getDestination() {
        return destination;
    }

    public Host getSource() {
        return source;
    }

    @Override
    public String toString() {
        String srcMacStr = source.mac().toString();
        String dstMacStr = destination.mac().toString();
        String str = "HostToHost (" + srcMacStr + " -> " + dstMacStr + ")";
        return str;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HostToHostIntent) {
            final HostToHostIntent other = (HostToHostIntent) obj;
            if (this.getModuleId() != other.getModuleId()) {
                return false;
            }
            if (! this.source.equals(other.source) || ! this.destination.equals(other.destination)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return source.hashCode() * destination.hashCode();
    }
}
