package eu.netide.core.globalfib.intent;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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

import java.util.*;

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

    /**
     * A set of FlowModEntries together with the time they were added to the set.
     */
    private Set<Map.Entry<FlowModEntry, Long>> unassignedFlowModEntries = new HashSet<>();

    /**
     * The timeout for unassigned FlowModEntries in ms.
     */
    private static final long unassignedFlowModEntryTimeout = 10000;

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
                intents.add(newIntent);

                // Check if any currently unassigned FlowEntries match the new intent
                Iterator<Map.Entry<FlowModEntry, Long>> iterator = unassignedFlowModEntries.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<FlowModEntry, Long> entry = iterator.next();
                    FlowModEntry storedFlowModEntry = entry.getKey();
                    Long entryTime = entry.getValue();
                    if (flowModEntryMatchesIntent(newIntent, storedFlowModEntry)) {
                        newIntent.addFlowModEntry(storedFlowModEntry);
                        iterator.remove();
                    } else {
                        // Remove entry if could not be assigned and timed out.
                        if (entryTime - System.currentTimeMillis() > unassignedFlowModEntryTimeout) {
                            iterator.remove();
                        }
                    }
                }
            }
        } else { // Else, add FlowMod to all intents that match

            // Add FlowMod to all intents it applies to
            Set<Intent> matchingIntents = findMatchingIntents(flowModEntry);
            if (matchingIntents.isEmpty()) {
                // Store FlowModEntry for later
                Map.Entry<FlowModEntry, Long> entry = new AbstractMap.SimpleEntry<FlowModEntry, Long>(
                        flowModEntry, System.currentTimeMillis());
                unassignedFlowModEntries.add(entry);
                return;
            }
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

        Link srcLink = DefaultLink.builder()
                .providerId(ProviderId.NONE)
                .src(new ConnectPoint(((HostToHostIntent) intent).getSource().id(), PortNumber.portNumber(0)))
                .dst(((HostToHostIntent) intent).getSource().location())
                .type(Link.Type.EDGE)
                .state(Link.State.ACTIVE)
                .build();
        Link dstLink = DefaultLink.builder()
                .providerId(ProviderId.NONE)
                .src(((HostToHostIntent) intent).getDestination().location())
                .dst(new ConnectPoint(((HostToHostIntent) intent).getDestination().id(), PortNumber.portNumber(0)))
                .type(Link.Type.EDGE)
                .state(Link.State.ACTIVE)
                .build();

        Path path = null;
        if (srcSwitch.equals(dstSwitch)) {
            // Src and dst hosts connected to the same switch
            List<Link> links = new LinkedList<>();
            links.add(srcLink);
            links.add(dstLink);

            path = new DefaultPath(ProviderId.NONE, links, 0);
        } else {
            // Else find a path
            Set<Path> topologyPaths = topologyService.getPaths(
                    topologyService.currentTopology(), srcSwitch, dstSwitch);
            Path firstPath = topologyPaths.iterator().next();

            // Manually add links between src/dst hosts and switches
            List<Link> newLinks = new LinkedList<>();
            newLinks.add(srcLink);
            newLinks.addAll(firstPath.links());
            newLinks.add(dstLink);

            path = new DefaultPath(ProviderId.NONE, newLinks, firstPath.cost());
        }

        return path;
    }

    /**
     * Find all intents that match the FlowEntry.
     *
     * @param flowModEntry FlowModEntry to match against.
     * @return Set of intents matching the FlowEntry.
     */
    public Set<Intent> findMatchingIntents(FlowModEntry flowModEntry) {
        Set<Intent> matchingIntents = new HashSet<>();
        for (Intent intent : intents) {
            if (flowModEntryMatchesIntent(intent, flowModEntry)) {
                matchingIntents.add(intent);
            }
        }
        return matchingIntents;
    }

    /**
     * Tests if a FlowModEntry matches the given Intent.
     *
     * @param intent       Intent to check.
     * @param flowModEntry FlowModEntry to check.
     * @return true, if flowModEntry matches intent.
     */
    private boolean flowModEntryMatchesIntent(Intent intent, FlowModEntry flowModEntry) {
        OFFlowMod flowMod = flowModEntry.getFlowMod();
        HostToHostIntent hintent = (HostToHostIntent) intent;

        // Check if destination matches
        byte[] mac = flowMod.getMatch().get(MatchField.ETH_DST).getBytes();
        MacAddress macAddress = flowMod.getMatch().get(MatchField.ETH_DST);
        if (!hintent.getDestination().mac().equals(onosMacAddress(macAddress))) {
            return false;
        }

        // Check if module id matches
        if (intent.getModuleId() != flowModEntry.getModuleId()) {
            return false;
        }

        DeviceId deviceId = onosDeviceId(flowModEntry.getDpid());
        PortNumber outputPort = null;
        for (OFAction action : flowMod.getActions()) {
            if (action.getType() == OFActionType.OUTPUT) {
                outputPort = PortNumber.portNumber(((OFActionOutput) action).getPort().getPortNumber());
                break;
            }
        }
        PortNumber inputPort = onosPortNumber(flowMod.getMatch().get(MatchField.IN_PORT));

        // Check if flow mod lies on path through topology
        Path path = getIntentPath(intent);
        Iterator<Link> iterator = path.links().iterator();
        Link lastLink = null;
        while (iterator.hasNext()) {
            Link link = iterator.next();

            // Ignore link from source host to first switch
            if (link.src().elementId() instanceof HostId) {
                lastLink = link;
                continue;
            }
            if (link.src().deviceId().equals(deviceId)
                    && link.src().port().equals(outputPort)
                    && lastLink.dst().port().equals(inputPort)){
                return true;
            }
            lastLink = link;
        }
        return false;
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

    @Override
    public Set<Intent> getIntents() {
        return intents;
    }

    public void bindHostService(HostService hostService) {
        this.hostService = hostService;
    }

    public void bindTopologyService(TopologyService topologyService) {
        this.topologyService = topologyService;
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
