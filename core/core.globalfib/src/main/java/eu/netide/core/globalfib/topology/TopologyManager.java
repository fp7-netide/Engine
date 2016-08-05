package eu.netide.core.globalfib.topology;

import org.apache.felix.scr.annotations.*;
import org.onosproject.common.DefaultTopology;
import org.onosproject.common.DefaultTopologyGraph;
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
     * The graph containing the imported topology.
     */
    private TopologyGraph topologyGraph;

    /**
     * Current topology. This object is just a dummy and never actually used.
     */
    private Topology topology;

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
        Map<String, TopologyVertex> vertices = new HashMap<>();
        Set<TopologyEdge> edges = new HashSet<>();

        for (Switch sw : topologySpecification.getSwitches()) {
            TopologyVertex vertex = new DefaultTopologyVertex(
                    DeviceId.deviceId("of:" + sw.getDpid()));
            vertices.put(sw.getId(), vertex);
        }
        for (eu.netide.core.globalfib.topology.Link link : topologySpecification.getLinks()) {
            // Ignore edge links
            if (!vertices.containsKey(link.getSource()) || !vertices.containsKey(link.getDestination())) {
                continue;
            }

            ConnectPoint src = new ConnectPoint(
                    vertices.get(link.getSource()).deviceId(),
                    PortNumber.portNumber(link.getSourcePort()));
            ConnectPoint dst = new ConnectPoint(
                    vertices.get(link.getDestination()).deviceId(),
                    PortNumber.portNumber(link.getDestinationPort()));

            Link newLink = new DefaultLink(ProviderId.NONE, src, dst,
                    Link.Type.DIRECT, Link.State.ACTIVE, true);
            TopologyEdge edge = new DefaultTopologyEdge(
                    vertices.get(link.getSource()), vertices.get(link.getDestination()),
                    newLink);
            edges.add(edge);
        }
        topologyGraph = new DefaultTopologyGraph(new HashSet<>(vertices.values()), edges);
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
        return topologyGraph;
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
        return null;
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
