package eu.netide.core.api.netip;

/**
 * Created by timvi on 06.08.2015.
 */
public class NetconfMessage extends Message {
    public byte[] toByteRepresentation() {
        byte[] bytes = new byte[16 + this.payload.length];
        System.arraycopy(header.toByteRepresentation(), 0, bytes, 0, 16);
        System.arraycopy(payload, 0, bytes, 16, this.payload.length);
        return bytes;
    }
}
