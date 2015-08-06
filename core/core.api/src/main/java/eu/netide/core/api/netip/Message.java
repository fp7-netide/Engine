package eu.netide.core.api.netip;

/**
 * Created by timvi on 06.08.2015.
 */
public class Message {
    protected MessageHeader header;
    protected byte[] payload;

    public MessageHeader getHeader() {
        return header;
    }

    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] toByteRepresentation() {
        byte[] bytes = new byte[16 + payload.length];
        System.arraycopy(header.toByteRepresentation(), 0, bytes, 0, 16);
        System.arraycopy(payload, 0, bytes, 16, payload.length);
        return bytes;
    }
}
