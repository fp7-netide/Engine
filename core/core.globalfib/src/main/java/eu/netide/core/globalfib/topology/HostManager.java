package eu.netide.core.globalfib.topology;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.*;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by msp on 7/11/16.
 */
@Component(immediate = true)
@Service
public class HostManager implements HostService {

    private Map<HostId, Host> hosts;

    public HostManager() {
        hosts = new HashMap<>();
    }

    /**
     * Reads hosts and connection points from the given TopologySpecification.
     * @param topologySpecification TopologySpecifiaction to use.
     */
    public void setTopologySpecification(TopologySpecification topologySpecification) {
        for (eu.netide.core.globalfib.topology.Host host : topologySpecification.getHosts()) {
            MacAddress macAddress = MacAddress.valueOf(host.getMac());
            HostId hostId = HostId.hostId(macAddress);

            Link edgeLink = (Link) topologySpecification.getAdjacentLinks(host.getId()).toArray()[0];
            Switch sw;
            long port;
            if (edgeLink.getSource().equals(host.getId())) {
                sw = topologySpecification.getSwitch(edgeLink.getDestination());
                port = edgeLink.getDestinationPort();
            } else {
                sw = topologySpecification.getSwitch(edgeLink.getSource());
                port = edgeLink.getSourcePort();
            }
            ConnectPoint connectPoint = new ConnectPoint(
                    DeviceId.deviceId("of:" + sw.getDpid()),
                    PortNumber.portNumber(port)
                    );
            HostLocation hostLocation = new HostLocation(connectPoint, System.currentTimeMillis());

            Set<IpAddress> ipAddresses = new HashSet<>();
            ipAddresses.add(IpAddress.valueOf(host.getIp()));

            Host newHost = new DefaultHost(
                    ProviderId.NONE,
                    hostId,
                    macAddress,
                    hostId.vlanId(),
                    hostLocation,
                    ipAddresses
            );

            hosts.put(hostId, newHost);
        }
    }

    @Override
    public int getHostCount() {
        return hosts.size();
    }

    @Override
    public Iterable<Host> getHosts() {
        return hosts.values();
    }

    @Override
    public Host getHost(HostId hostId) {
        return hosts.get(hostId);
    }

    @Override
    public Set<Host> getHostsByVlan(VlanId vlanId) {
        Set<Host> matchingHosts = new HashSet<>();
        for (Host host : hosts.values()) {
            if (host.vlan().equals(vlanId)) {
                matchingHosts.add(host);
            }
        }
        return matchingHosts;
    }

    @Override
    public Set<Host> getHostsByMac(MacAddress mac) {
        Set<Host> matchingHosts = new HashSet<>();
        for (Host host : hosts.values()) {
            if (host.mac().equals(mac)) {
                matchingHosts.add(host);
            }
        }
        return matchingHosts;
    }

    @Override
    public Set<Host> getHostsByIp(IpAddress ip) {
        Set<Host> matchingHosts = new HashSet<>();
        for (Host host : hosts.values()) {
            if (host.ipAddresses().equals(ip)) {
                matchingHosts.add(host);
            }
        }
        return matchingHosts;
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        Set<Host> matchingHosts = new HashSet<>();
        for (Host host : hosts.values()) {
            if (host.location().equals(connectPoint)) {
                matchingHosts.add(host);
            }
        }
        return matchingHosts;
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        Set<Host> matchingHosts = new HashSet<>();
        for (Host host : hosts.values()) {
            if (host.location().deviceId().equals(deviceId)) {
                matchingHosts.add(host);
            }
        }
        return matchingHosts;
    }

    @Override
    public void startMonitoringIp(IpAddress ip) {

    }

    @Override
    public void stopMonitoringIp(IpAddress ip) {

    }

    @Override
    public void requestMac(IpAddress ip) {

    }

    @Override
    public void addListener(HostListener listener) {

    }

    @Override
    public void removeListener(HostListener listener) {

    }
}
