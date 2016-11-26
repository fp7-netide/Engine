package eu.netide.core.globalfib.topology;

import org.apache.felix.scr.annotations.*;
import org.onosproject.common.DefaultTopology;
import org.onosproject.net.*;
import org.onosproject.net.Link;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.*;

import java.util.*;

/**
 * Created by msp on 7/7/16.
 */
@Component(immediate = true)
@Service
public class TopologyManager implements TopologyService {

    /**
     * Current topology. This object is just a dummy and never actually used.
     */
    private DefaultTopology topology;

    public TopologyManager() {
        GraphDescription graphDescription = new DefaultGraphDescription(
                System.nanoTime(),
                System.currentTimeMillis(),
                new LinkedList<Device>(),
                new LinkedList<Link>()
        );
        topology = new DefaultTopology(ProviderId.NONE, graphDescription);
    }

    /**
     * Creates a TopologyGraph from the given TopologySpecification.
     * @param topologySpecification TopologySpecifiaction to use.
     */
    public void setTopologySpecification(TopologySpecification topologySpecification) {
        Map<String, Device> vertices = new HashMap<>();

        List<Device> devices = new LinkedList<>();
        List<Link> links = new LinkedList<>();

        for (Switch sw : topologySpecification.getSwitches()) {
            Device device = new DefaultDevice(
                    ProviderId.NONE,
                    DeviceId.deviceId("of:" + sw.getDpid()),
                    Device.Type.SWITCH,
                    null, null, null, null, null);
            devices.add(device);

            TopologyVertex vertex = new DefaultTopologyVertex(
                    DeviceId.deviceId("of:" + sw.getDpid()));
            vertices.put(sw.getId(), device);
        }
        for (eu.netide.core.globalfib.topology.Link link : topologySpecification.getLinks()) {
            // Ignore edge links
            if (!vertices.containsKey(link.getSource()) || !vertices.containsKey(link.getDestination())) {
                continue;
            }

            ConnectPoint src = new ConnectPoint(
                    vertices.get(link.getSource()).id(),
                    PortNumber.portNumber(link.getSourcePort()));
            ConnectPoint dst = new ConnectPoint(
                    vertices.get(link.getDestination()).id(),
                    PortNumber.portNumber(link.getDestinationPort()));

            // Add directed links in both directions
            Link newLink1 = DefaultLink.builder()
                    .providerId(ProviderId.NONE)
                    .src(src)
                    .dst(dst)
                    .type(Link.Type.DIRECT)
                    .state(Link.State.ACTIVE)
                    .build();
            Link newLink2 = DefaultLink.builder()
                    .providerId(ProviderId.NONE)
                    .src(dst)
                    .dst(src)
                    .type(Link.Type.DIRECT)
                    .state(Link.State.ACTIVE)
                    .build();
            links.add(newLink1);
            links.add(newLink2);
        }
        GraphDescription graphDescription = new DefaultGraphDescription(
                System.nanoTime(),
                System.currentTimeMillis(),
                devices,
                links
        );
        topology = new DefaultTopology(ProviderId.NONE, graphDescription);
    }

    @Override
    public Topology currentTopology() {
        return topology;
    }

    @Override
    public boolean isLatest(Topology topology) {
        return true;
    }

    @Override
    public TopologyGraph getGraph(Topology topology) {
        return ((DefaultTopology) topology).getGraph();
    }

    @Override
    public Set<TopologyCluster> getClusters(Topology topology) {
        return null;
    }

    @Override
    public TopologyCluster getCluster(Topology topology, ClusterId clusterId) {
        return null;
    }

    @Override
    public Set<DeviceId> getClusterDevices(Topology topology, TopologyCluster cluster) {
        return null;
    }

    @Override
    public Set<org.onosproject.net.Link> getClusterLinks(Topology topology, TopologyCluster cluster) {
        return null;
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        if (! (topology instanceof DefaultTopology)) {
            return null;
        }

        DefaultTopology defaultTopology = (DefaultTopology) topology;
        return defaultTopology.getPaths(src, dst);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
        return getPaths(topology, src, dst);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
        return getDisjointPaths(topology, src, dst);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst, Map<org.onosproject.net.Link, Object> riskProfile) {
        return getDisjointPaths(topology, src, dst);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight, Map<Link, Object> riskProfile) {
        return getDisjointPaths(topology, src, dst);
    }

    @Override
    public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        return false;
    }

    @Override
    public boolean isBroadcastPoint(Topology topology, ConnectPoint connectPoint) {
        return false;
    }

    @Override
    public void addListener(TopologyListener listener) {

    }

    @Override
    public void removeListener(TopologyListener listener) {

    }
}
