package eu.netide.lib.netip;

/**
 * Created by timvi on 06.08.2015.
 */
public enum NetIDEProtocolVersion {
    /**
     * The Version 1.0
     */
    VERSION_1_0((byte) 0x01),
    /**
     * The Version 1.1
     */
    VERSION_1_1((byte) 0x02),
    /**
     * The Version 1.2
     */
    VERSION_1_2((byte) 0x03),
    /**
     * The Version 1.3
     */
    VERSION_1_3((byte) 0x04);

    private byte value;

    /**
     * Instantiates a new Net iDE protocol version.
     *
     * @param value the value
     */
    NetIDEProtocolVersion(byte value) {
        this.value = value;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public byte getValue() {
        return this.value;
    }

    /**
     * Parse net iDE protocol version.
     *
     * @param value the value
     * @return the net iDE protocol version
     */
    public static NetIDEProtocolVersion parse(final byte value) {
        for (NetIDEProtocolVersion c : NetIDEProtocolVersion.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
