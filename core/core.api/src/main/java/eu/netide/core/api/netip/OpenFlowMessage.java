package eu.netide.core.api.netip;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Created by timvi on 06.08.2015.
 */
public class OpenFlowMessage extends Message {
    private OFMessage ofMessage;

    public OFMessage getOfMessage() {
        return ofMessage;
    }

    public void setOfMessage(OFMessage ofMessage) {
        this.ofMessage = ofMessage;
    }

    public byte[] toByteRepresentation() {
        ChannelBuffer dcb = ChannelBuffers.dynamicBuffer();
        ofMessage.writeTo(dcb);
        this.payload = dcb.array();

        byte[] bytes = new byte[16 + this.payload.length];
        System.arraycopy(header.toByteRepresentation(), 0, bytes, 0, 16);
        System.arraycopy(payload, 0, bytes, 16, this.payload.length);
        return bytes;
    }
}
