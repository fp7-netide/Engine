package eu.netide.lib.netip;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a message of type ERROR.
 * Note that this only serves as a convenience class - if the MessageType is manipulated, the class will not recognize that.
 */
public class ErrorMessage extends Message {
    /**
     * The list of supported protocols and their versions.
     */
    private List<Pair<Protocol, ProtocolVersions>> supportedProtocols;

    /**
     * Instantiates a new Error message.
     */
    public ErrorMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.ERROR);
        supportedProtocols = new ArrayList<>();
    }

    /**
     * Gets supported protocols.
     *
     * @return the supported protocols
     */
    public List<Pair<Protocol, ProtocolVersions>> getSupportedProtocols() {
        return supportedProtocols;
    }

    /**
     * Sets supported protocols.
     *
     * @param supportedProtocols the supported protocols
     */
    public void setSupportedProtocols(List<Pair<Protocol, ProtocolVersions>> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public byte[] getPayload() {
        byte[] payload = new byte[supportedProtocols.size() * 2];
        int i = 0;
        for (Pair<Protocol, ProtocolVersions> entry : supportedProtocols) {
            payload[i] = entry.getValue0().getValue();
            payload[i + 1] = entry.getValue1().getValue();
            i += 2;
        }
        this.payload = payload;
        return this.payload;
    }
}
