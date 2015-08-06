package eu.netide.core.api.netip;

/**
 * Created by timvi on 06.08.2015.
 */
public enum ProtocolVersion {
    VERSION1((byte) 0x01);

    private byte value;

    ProtocolVersion(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static ProtocolVersion parse(final byte value) {
        for (ProtocolVersion c : ProtocolVersion.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
