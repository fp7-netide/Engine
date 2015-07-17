package org.onosproject.shim.message;

public class NetIDEHeader {
	final int NETIDE_HEADER_LENGHT = 16;
	
	byte netIDEVersion;
	NetIDEType type;
	short lenght;
	int xid;
	long datapathId;
	
	public NetIDEHeader() {
		netIDEVersion = 0x00;
		type = NetIDEType.NETIDE_NOTSUPPORTED;
		lenght = 0;
		xid = 0;
		datapathId = 0;
	}

	public int getHeaderLenght() {
		return NETIDE_HEADER_LENGHT;
	}
	
	public byte getNetIDEVersion() {
		return netIDEVersion;
	}

	public void setNetIDEVersion(byte netIDEVersion) {
		this.netIDEVersion = netIDEVersion;
	}

	public NetIDEType getType() {
		return type;
	}

	public void setType(byte value) {
		if (value == NetIDEType.NETIDE_ERROR.getNetIDETypeId())
			this.type = NetIDEType.NETIDE_ERROR;
		else if (value == NetIDEType.NETIDE_HELLO.getNetIDETypeId())
			this.type = NetIDEType.NETIDE_HELLO;
		else if (value == NetIDEType.NETIDE_NETCONF.getNetIDETypeId())
			this.type = NetIDEType.NETIDE_NETCONF;
		else if (value == NetIDEType.NETIDE_OPENFLOW.getNetIDETypeId())
			this.type = NetIDEType.NETIDE_OPENFLOW;
		else if (value == NetIDEType.NETIDE_OPFLEX.getNetIDETypeId())
			this.type = NetIDEType.NETIDE_OPFLEX;
		else this.type = NetIDEType.NETIDE_NOTSUPPORTED;
	}

	public short getLenght() {
		return lenght;
	}

	public void setLenght(short lenght) {
		this.lenght = lenght;
	}

	public int getXid() {
		return xid;
	}

	public void setXid(int xid) {
		this.xid = xid;
	}

	public long getDatapathId() {
		return datapathId;
	}

	public void setDatapathId(long datapathId) {
		this.datapathId = datapathId;
	}

	public String toString() {
		return "netIDEVersion: " + Integer.toHexString(netIDEVersion) + " type: " + type + 
				" lenght: " + lenght + " xid: " + Integer.toHexString(xid) + " datapathId: " + Long.toHexString(datapathId);
	}
	
	
}
