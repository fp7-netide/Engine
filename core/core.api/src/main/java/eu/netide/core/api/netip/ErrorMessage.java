package eu.netide.core.api.netip;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by timvi on 06.08.2015.
 */
public class ErrorMessage extends Message {
    private Dictionary<Protocol, Byte> supportedProtocols;

    public ErrorMessage() {
        header = new MessageHeader();
        header.setMessageType(MessageType.ERROR);
        supportedProtocols = new Hashtable<>();
    }

    public Dictionary<Protocol, Byte> getSupportedProtocols() {
        return supportedProtocols;
    }

    public void setSupportedProtocols(Dictionary<Protocol, Byte> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    public byte[] toByteRepresentation() {
        byte[] payload = new byte[supportedProtocols.size() * 2];
        Enumeration<Protocol> keys = supportedProtocols.keys();
        int i = 0;
        while (keys.hasMoreElements()) {
            Protocol current = keys.nextElement();
            payload[i] = current.getValue();
            payload[i + 1] = supportedProtocols.get(current);
            i += 2;
        }
        this.payload = payload;

        byte[] bytes = new byte[16 + this.payload.length];
        System.arraycopy(header.toByteRepresentation(), 0, bytes, 0, 16);
        System.arraycopy(payload, 0, bytes, 16, this.payload.length);
        return bytes;
    }
}
