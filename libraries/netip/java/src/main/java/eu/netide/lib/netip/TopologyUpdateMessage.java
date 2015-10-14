package eu.netide.lib.netip;

/**
 * Created by timvi on 24.09.2015.
 */
public class TopologyUpdateMessage extends Message {

    private String topology;

    /**
     * Instantiates a new Topology update message.
     */
    public TopologyUpdateMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.TOPOLOGY_UPDATE);
    }

    /**
     * Gets topology.
     *
     * @return the topology
     */
    public String getTopology() {
        return topology;
    }

    /**
     * Sets topology.
     *
     * @param topology the topology
     */
    public void setTopology(String topology) {
        this.topology = topology;
    }

    @Override
    public byte[] getPayload() {
        return topology.getBytes();
    }
}
