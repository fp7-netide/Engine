package eu.netide.lib.netip;

import org.javatuples.Pair;
import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;

import java.nio.ByteBuffer;

/**
 * Utility methods for handling NetIP messages.
 */
public abstract class NetIPUtils {
    /**
     * Gets a stub header from the given payload with the length correctly set and protocol version set to 1.1.
     *
     * @param payload the payload
     * @return the message header
     */
    public static MessageHeader StubHeaderFromPayload(byte[] payload) {
        MessageHeader h = new MessageHeader();
        h.setPayloadLength((short) payload.length);
        h.setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        return h;
    }

    /**
     * Concretizes the given message to the corresponding convenience class.
     *
     * @param message the message
     * @return the concretized message (e.g. an instance of OpenFlowMessage)
     */
    public static Message ConcretizeMessage(Message message) {
        try {
            switch (message.getHeader().getMessageType()) {
                case HELLO:
                    return toHelloMessage(message);
                case ERROR:
                    return toErrorMessage(message);
                case OPENFLOW:
                    return toOpenFlowMessage(message);
                case NETCONF:
                    return toNetconfMessage(message);
                case OPFLEX:
                    return toOpFlexMessage(message);
                case MANAGEMENT:
                    return toManagementMessage(message);
                default:
                    throw new IllegalArgumentException("Unknown message type.");
            }
        } catch (OFParseError ofpe) {
            throw new IllegalArgumentException("Could not decode OpenFlow message.", ofpe);
        }
    }

    /**
     * To hello message.
     *
     * @param message the message
     * @return the hello message
     */
    private static HelloMessage toHelloMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.HELLO)
            throw new IllegalArgumentException("Can only convert HELLO messages");
        HelloMessage hm = new HelloMessage();
        hm.setHeader(message.header);
        for (int i = 0; i < message.getPayload().length; i += 2) {
            Protocol protocol = Protocol.parse(message.getPayload()[i]);
            hm.getSupportedProtocols().add(new Pair<>(protocol, ProtocolVersions.parse(protocol, message.getPayload()[i + 1])));
        }
        return hm;
    }

    /**
     * To error message.
     *
     * @param message the message
     * @return the error message
     */
    private static ErrorMessage toErrorMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.ERROR)
            throw new IllegalArgumentException("Can only convert ERROR messages");
        ErrorMessage em = new ErrorMessage();
        em.setHeader(message.header);
        for (int i = 0; i < message.getPayload().length; i += 2) {
            Protocol protocol = Protocol.parse(message.getPayload()[i]);
            em.getSupportedProtocols().add(new Pair<>(protocol, ProtocolVersions.parse(protocol, message.getPayload()[i + 1])));
        }
        return em;
    }

    /**
     * To open flow message.
     *
     * @param message the message
     * @return the open flow message
     * @throws OFParseError the oF parse error
     */
    private static OpenFlowMessage toOpenFlowMessage(Message message) throws OFParseError {
        if (message.getHeader().getMessageType() != MessageType.OPENFLOW)
            throw new IllegalArgumentException("Can only convert OPENFLOW messages");
        OpenFlowMessage ofm = new OpenFlowMessage();
        ofm.setHeader(message.header);
        ofm.setOfMessage(OFFactories.getGenericReader().readFrom(new ByteBufferBackedChannelBuffer(ByteBuffer.wrap(message.payload))));
        return ofm;
    }

    /**
     * To netconf message.
     *
     * @param message the message
     * @return the netconf message
     */
    private static NetconfMessage toNetconfMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.NETCONF)
            throw new IllegalArgumentException("Can only convert NETCONF messages");
        NetconfMessage ncm = new NetconfMessage();
        ncm.setHeader(message.header);
        ncm.setPayload(message.payload);
        return ncm;
    }

    /**
     * To op flex message.
     *
     * @param message the message
     * @return the op flex message
     */
    private static OpFlexMessage toOpFlexMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.OPFLEX)
            throw new IllegalArgumentException("Can only convert OPFLEX messages");
        OpFlexMessage ofm = new OpFlexMessage();
        ofm.setHeader(message.header);
        ofm.setPayload(message.payload);
        return ofm;
    }

    /**
     * To ManagementMessage.
     *
     * @param message the message
     * @return the management message
     */
    private static ManagementMessage toManagementMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.MANAGEMENT)
            throw new IllegalArgumentException("Can only convert MANAGEMENT messages");
        ManagementMessage ofm = new ManagementMessage();
        ofm.setHeader(message.header);
        ofm.setPayloadString(new String(message.getPayload()));
        return ofm;
    }
}
