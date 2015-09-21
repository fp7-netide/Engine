package org.opendaylight.netide.shim;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.ThreadConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.TlsConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.TransportProtocol;

public class ShimConnectionConfiguration implements ConnectionConfiguration{
	
	private int port = 6633;
	
	@Override
	public InetAddress getAddress() {
		// TODO Auto-generated method stub
		InetAddress addr = null;
		
		try {
			addr = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return addr;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public Object getSslContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSwitchIdleTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ThreadConfiguration getThreadConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TlsConfiguration getTlsConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getTransferProtocol() {
		// TODO Auto-generated method stub
		return TransportProtocol.TCP;
	}

}
