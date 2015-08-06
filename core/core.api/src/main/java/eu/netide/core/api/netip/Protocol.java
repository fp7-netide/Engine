package eu.netide.core.api.netip;

/**
 * Created by timvi on 06.08.2015.
 */
public enum Protocol {
    OPENFLOW((byte) 2),
    NETCONF((byte) 3),
    OPFLEX((byte) 4);

    private byte value;

    Protocol(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static Protocol parse(final byte value) {
        for (Protocol c : Protocol.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
