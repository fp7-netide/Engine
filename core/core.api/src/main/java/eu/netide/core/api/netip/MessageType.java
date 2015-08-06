package eu.netide.core.api.netip;

/**
 * Created by timvi on 06.08.2015.
 */
public enum MessageType {
    HELLO((byte) 0),
    ERROR((byte) 1),
    OPENFLOW(Protocol.OPENFLOW.getValue()),
    NETCONF(Protocol.NETCONF.getValue()),
    OPFLEX(Protocol.OPFLEX.getValue());

    private byte value;

    MessageType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static MessageType parse(final byte value) {
        for (MessageType c : MessageType.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
