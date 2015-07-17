package org.onosproject.shim.message;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedList;

import org.slf4j.Logger;

public class NetIDEHello {
	private final Logger log = getLogger(getClass());
	
	LinkedList<NetIDEProtocol> supportedProtocols = new LinkedList<>();
	
	public NetIDEHello(byte[] data) {
		for (short i = 0; i < data.length; i+= 2) {
			supportedProtocols.add(setProtocol(data[i], data[i+1]));
		}
	}
	
	
	private NetIDEProtocol setProtocol(byte type, byte protocol) {
		if (type == NetIDEType.NETIDE_NETCONF.getNetIDETypeId() && 
				protocol == NetIDEProtocol.NETCONF_1_0.getNetIDEProtocolValue())
			return NetIDEProtocol.NETCONF_1_0;
		else if (type == NetIDEType.NETIDE_OPFLEX.getNetIDETypeId() && 
				protocol == NetIDEProtocol.OPFLEX_0_0.getNetIDEProtocolValue())
			return NetIDEProtocol.OPFLEX_0_0;
		else if (type == NetIDEType.NETIDE_OPENFLOW.getNetIDETypeId() && 
				protocol == NetIDEProtocol.OPENFLOW_1_1.getNetIDEProtocolValue())
			return NetIDEProtocol.OPENFLOW_1_1;
		else if (type == NetIDEType.NETIDE_OPENFLOW.getNetIDETypeId() && 
				protocol == NetIDEProtocol.OPENFLOW_1_2.getNetIDEProtocolValue())
			return NetIDEProtocol.OPENFLOW_1_2;
		else if (type == NetIDEType.NETIDE_OPENFLOW.getNetIDETypeId() && 
				protocol == NetIDEProtocol.OPENFLOW_1_3.getNetIDEProtocolValue())
			return NetIDEProtocol.OPENFLOW_1_3;
		else if (type == NetIDEType.NETIDE_OPENFLOW.getNetIDETypeId() && 
				protocol == NetIDEProtocol.OPENFLOW_1_4.getNetIDEProtocolValue())
			return NetIDEProtocol.OPENFLOW_1_4;
		else 
			return NetIDEProtocol.OPENFLOW_1_0;

	}
	
	public LinkedList<NetIDEProtocol> getSupportedProtocols() {
		return this.supportedProtocols;
	}
	
	public String toString() {
		String res = "";
		for (NetIDEProtocol proto : this.supportedProtocols) {
			res = res + proto + " ";
		}
		
		return res;
	}

}
