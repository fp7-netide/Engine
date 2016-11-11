package eu.netide.core.globalfib.intent;

import eu.netide.core.globalfib.FlowModEntry;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.common.DefaultTopology;
import org.onosproject.common.DefaultTopologyGraph;
import org.onosproject.net.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.*;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.MacAddress;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by msp on 9/15/16.
 */
@Component(immediate = true)
@Service
public class IntentManager implements IntentService {

    private Set<Intent> intents = new HashSet<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Override
    public void process(FlowModEntry flowModEntry) {
        DeviceId deviceId = onosDeviceId(flowModEntry.getDpid());
        PortNumber onosInPort = onosPortNumber(
                flowModEntry.getFlowMod().getMatch().get(MatchField.IN_PORT));
        org.onlab.packet.MacAddress onosEthDst = onosMacAddress(
                flowModEntry.getFlowMod().getMatch().get(MatchField.ETH_DST));

        // If FlowMod represents host to switch edge link create new intent if necessary
        Host source = findConnectedHost(deviceId, onosInPort);
        if (source != null) {
            HostId destinationHostId = HostId.hostId(onosEthDst);
            Host destination = hostService.getHost(destinationHostId);

            HostToHostIntent newIntent = new HostToHostIntent(
                    flowModEntry.getModuleId(), source, destination);
            // Check if the intent already exists
            if (intents.contains(newIntent)) {
                return;
            } else {
                newIntent.addFlowModEntry(flowModEntry);
                // TODO: Check if any stored unassociated FlowEntry matches the new intent.
                intents.add(newIntent);
            }
        } else { // Else, add FlowMod to all intents that match

            // Add FlowMod to all intents it applies to
            Set<Intent> matchingIntents = findMatchingIntents(flowModEntry);
            for (Intent intent : matchingIntents) {
                if (!(intent instanceof HostToHostIntent)) {
                    continue;
                }

                intent.addFlowModEntry(flowModEntry);
            }
        }
    }

    /**
     * Returns a path through the topology that matches the intent.
     *
     * @param intent Intent to find path for.
     * @return Path through topology.
     */
    private Path getIntentPath(Intent intent) {
        DeviceId srcSwitch = ((HostToHostIntent) intent).getSource().location().deviceId();
        DeviceId dstSwitch = ((HostToHostIntent) intent).getDestination().location().deviceId();

        Set<Path> topologyPaths = topologyService.getPaths(
                topologyService.currentTopology(), srcSwitch, dstSwitch);
        Path path = topologyPaths.iterator().next();

        // Manually add links between src/dst hosts and switches
        if (path != null) {
            Link srcLink = new DefaultLink(
                    ProviderId.NONE,
                    new ConnectPoint(((HostToHostIntent) intent).getSource().id(), PortNumber.portNumber(0)),
                    ((HostToHostIntent) intent).getSource().location(),
                    Link.Type.EDGE,
                    Link.State.ACTIVE,
                    true);
            Link dstLink = new DefaultLink(
                    ProviderId.NONE,
                    ((HostToHostIntent) intent).getDestination().location(),
                    new ConnectPoint(((HostToHostIntent) intent).getDestination().id(), PortNumber.portNumber(0)),
                    Link.Type.EDGE,
                    Link.State.ACTIVE,
                    true);

            List<Link> newLinks = new LinkedList<>();
            newLinks.add(srcLink);
            newLinks.addAll(path.links());
            newLinks.add(dstLink);

            return new DefaultPath(ProviderId.NONE, newLinks, path.cost());
        }

        return path;
    }

    /**
     * Find all intents that match the FlowEntry.
     *
     * @param flowEntry FlowEntry to match against.
     * @return Set of intents matching the FlowEntry.
     */
    public Set<Intent> findMatchingIntents(FlowModEntry flowEntry) {
        OFFlowMod flowMod = flowEntry.getFlowMod();
        Set<Intent> matchingIntents = new HashSet<>();
        for (Intent intent : intents) {
            HostToHostIntent hintent = (HostToHostIntent) intent;

            // Check if destination matches
            byte[] mac = flowMod.getMatch().get(MatchField.ETH_DST).getBytes();
            MacAddress macAddress = flowMod.getMatch().get(MatchField.ETH_DST);
            if (!hintent.getDestination().mac().equals(onosMacAddress(macAddress))) {
                continue;
            }

            // Check if module id matches
            if (intent.getModuleId() != flowEntry.getModuleId()) {
                continue;
            }

            DeviceId deviceId = onosDeviceId(flowEntry.getDpid());
            PortNumber outputPort = null;
            for (OFAction action : flowMod.getActions()) {
                if (action.getType() == OFActionType.OUTPUT) {
                    outputPort = PortNumber.portNumber(((OFActionOutput) action).getPort().getPortNumber());
                    break;
                }
            }

            // Check if flow mod lies on path through topology
            Path path = getIntentPath(intent);
            for (Link link : path.links()) {
                // Ignore link from source host to first switch
                if (link.src().elementId() instanceof HostId) {
                    continue;
                }
                if (link.src().deviceId().equals(deviceId) && link.src().port().equals(outputPort)) {
                    matchingIntents.add(intent);
                    break;
                }
            }
        }
        return matchingIntents;
    }

    /**
     * Find Host connected to a device at a designated port.
     *
     * @param deviceId DeviceId of the device to check.
     * @param inPort   Port of the device to check.
     * @return Host connected to the port or null if not found.
     */
    private Host findConnectedHost(DeviceId deviceId, PortNumber inPort) {
        for (Host connectedHost : hostService.getConnectedHosts(deviceId)) {
            if (connectedHost.location().port().equals(inPort)) {
                return connectedHost;
            }
        }
        return null;
    }

    public void bindHostService(HostService hostService) {
        this.hostService = hostService;
    }

    public void bindTopologyService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    public Set<Intent> getIntents() {
        return intents;
    }

    private static PortNumber onosPortNumber(OFPort port) {
        return PortNumber.portNumber(port.getPortNumber());
    }

    private static org.onlab.packet.MacAddress onosMacAddress(MacAddress macAddress) {
        return org.onlab.packet.MacAddress.valueOf(macAddress.getBytes());
    }

    private static DeviceId onosDeviceId(long datapathId) {
        return DeviceId.deviceId(Dpid.uri(datapathId));
    }
}
