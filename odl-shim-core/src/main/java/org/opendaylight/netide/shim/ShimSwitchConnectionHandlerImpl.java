package org.opendaylight.netide.shim;

import java.net.InetAddress;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.connection.SwitchConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;


public class ShimSwitchConnectionHandlerImpl implements SwitchConnectionHandler {  
	
	private static final Logger LOG = LoggerFactory.getLogger(ShimSwitchConnectionHandlerImpl.class);
	private static ZMQ.Socket socket;
	
	public ShimSwitchConnectionHandlerImpl(ZMQ.Socket socket){
		this.socket = socket;
	}
	
	@Override
	public boolean accept(InetAddress arg0) {
		return true;
	}

	@Override
	public void onSwitchConnected(ConnectionAdapter connectionAdapter) {
		LOG.info("SHIM: on Switch connected");
		ShimMessageListener listener = new ShimMessageListener(socket);
		connectionAdapter.setMessageListener(listener);
		connectionAdapter.setSystemListener(listener);
		connectionAdapter.setConnectionReadyListener(listener);
	}
}
