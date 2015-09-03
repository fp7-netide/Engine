package eu.netide.lib.netip;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Class representing a message of type OPENFLOW.
 * Note that this only serves as a convenience class - if the MessageType is manipulated, the class will not recognize that.
 */
public class OpenFlowMessage extends Message {
    private OFMessage ofMessage;

    /**
     * Instantiates a new Open flow message.
     */
    public OpenFlowMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.OPENFLOW);
    }

    /**
     * Gets of message.
     *
     * @return the of message
     */
    public OFMessage getOfMessage() {
        return ofMessage;
    }

    /**
     * Sets of message.
     *
     * @param ofMessage the of message
     */
    public void setOfMessage(OFMessage ofMessage) {
        this.ofMessage = ofMessage;
    }

    @Override
    public byte[] getPayload() {
        ChannelBuffer dcb = ChannelBuffers.dynamicBuffer();
        ofMessage.writeTo(dcb);
        this.payload = dcb.array();
        return this.payload;
    }
}
