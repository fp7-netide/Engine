package eu.netide.lib.netip;

/**
 * Enumeration of known protocols, mapped to their respective representation value, as specified in the NetIDE intermediate protocol specification.
 */
public enum Protocol {
    /**
     * The OPENFLOW protocol.
     */
    OPENFLOW((byte) 0x11),
    /**
     * The NETCONF protocol.
     */
    NETCONF((byte) 0x12),
    /**
     * The OPFLEX protocol.
     */
    OPFLEX((byte) 0x13);

    private byte value;

    /**
     * Instantiates a new Protocol.
     *
     * @param value the value
     */
    Protocol(byte value) {
        this.value = value;
    }

    /**
     * Gets the value of this protocol in byte representations.
     *
     * @return the value
     */
    public byte getValue() {
        return this.value;
    }

    /**
     * Parse protocol.
     *
     * @param value the value
     * @return the protocol
     */
    public static Protocol parse(final byte value) {
        for (Protocol c : Protocol.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
