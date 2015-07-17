package org.onosproject.shim.message;


public enum NetIDEType {
	NETIDE_HELLO((byte)0x01),
	NETIDE_ERROR((byte)0x40),
	NETIDE_OPENFLOW((byte)0x11),
	NETIDE_NETCONF((byte)0x12),
	NETIDE_OPFLEX((byte)0x13),
	NETIDE_NOTSUPPORTED((byte)0x00);

    private final byte id;

    NetIDEType(byte id)
    {
        this.id = id;
    }
    
    public byte getNetIDETypeId() {
    	return this.id;
    }
    
}