package org.opendaylight.netide.shim;

import java.net.InetAddress;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionReadyListener;
import org.opendaylight.openflowjava.protocol.api.connection.SwitchConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShimSwitchConnectionHandlerImpl implements SwitchConnectionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(ShimSwitchConnectionHandlerImpl.class);
	
	@Override
	public boolean accept(InetAddress arg0) {
		return true;
	}

	@Override
	public void onSwitchConnected(ConnectionAdapter connectionAdapter) {
		LOG.info("SHIM: on Switch connected");
		ShimMessageListener listener = new ShimMessageListener();
		connectionAdapter.setMessageListener(listener);
		connectionAdapter.setSystemListener(listener);
		connectionAdapter.setConnectionReadyListener(listener);
	}

}
