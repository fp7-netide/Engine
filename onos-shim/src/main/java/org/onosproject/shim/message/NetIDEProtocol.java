package org.onosproject.shim.message;

public enum NetIDEProtocol {
	OPENFLOW_1_0 ((byte)0x01), 
	OPENFLOW_1_1 ((byte)0x02), 
	OPENFLOW_1_2 ((byte)0x03), 
	OPENFLOW_1_3 ((byte)0x04), 
	OPENFLOW_1_4 ((byte)0x05), 
	NETCONF_1_0 ((byte)0x01), 
	OPFLEX_0_0 ((byte)0x00);
	
	private final byte value;

	NetIDEProtocol(byte value)
    {
        this.value = value;
    }
    
    public byte getNetIDEProtocolValue() {
    	return this.value;
    }
    
}
