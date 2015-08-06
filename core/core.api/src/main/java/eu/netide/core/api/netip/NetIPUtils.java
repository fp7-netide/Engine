package eu.netide.core.api.netip;

import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;

import java.nio.ByteBuffer;

/**
 * Created by timvi on 06.08.2015.
 */
public abstract class NetIPUtils {
    public static MessageHeader StubHeaderFromPayload(byte[] payload) {
        MessageHeader h = new MessageHeader();
        h.setPayloadLength((short) payload.length);
        h.setProtocolVersion(ProtocolVersion.VERSION1);
        return h;
    }

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
                default:
                    throw new IllegalArgumentException("Unknown message type.");
            }
        } catch (OFParseError ofpe) {
            throw new IllegalArgumentException("Could not decode OpenFlow message.", ofpe);
        }
    }

    public static HelloMessage toHelloMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.HELLO)
            throw new IllegalArgumentException("Can only convert HELLO messages");
        HelloMessage hm = new HelloMessage();
        hm.setHeader(message.header);
        hm.setPayload(message.payload);
        for (int i = 0; i < hm.getPayload().length; i += 2) {
            hm.getSupportedProtocols().put(Protocol.parse(hm.getPayload()[i]), hm.getPayload()[i + 1]);
        }
        return hm;
    }

    public static ErrorMessage toErrorMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.ERROR)
            throw new IllegalArgumentException("Can only convert ERROR messages");
        ErrorMessage em = new ErrorMessage();
        em.setHeader(message.header);
        em.setPayload(message.payload);
        for (int i = 0; i < em.getPayload().length; i += 2) {
            em.getSupportedProtocols().put(Protocol.parse(em.getPayload()[i]), em.getPayload()[i + 1]);
        }
        return em;
    }

    public static OpenFlowMessage toOpenFlowMessage(Message message) throws OFParseError {
        if (message.getHeader().getMessageType() != MessageType.OPENFLOW)
            throw new IllegalArgumentException("Can only convert OPENFLOW messages");
        OpenFlowMessage ofm = new OpenFlowMessage();
        ofm.setHeader(message.header);
        ofm.setPayload(message.payload);
        ofm.setOfMessage(OFFactories.getGenericReader().readFrom(new ByteBufferBackedChannelBuffer(ByteBuffer.wrap(message.payload))));
        return ofm;
    }

    public static NetconfMessage toNetconfMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.NETCONF)
            throw new IllegalArgumentException("Can only convert NETCONF messages");
        NetconfMessage ncm = new NetconfMessage();
        ncm.setHeader(message.header);
        ncm.setPayload(message.payload);
        return ncm;
    }

    public static OpFlexMessage toOpFlexMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.OPFLEX)
            throw new IllegalArgumentException("Can only convert OPFLEX messages");
        OpFlexMessage ofm = new OpFlexMessage();
        ofm.setHeader(message.header);
        ofm.setPayload(message.payload);
        return ofm;
    }
}
