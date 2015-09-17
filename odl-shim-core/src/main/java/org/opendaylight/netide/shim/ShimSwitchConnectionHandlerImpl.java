package org.opendaylight.netide.shim;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.connection.SwitchConnectionHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInputBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.OpenFlowMessage;


public class ShimSwitchConnectionHandlerImpl implements SwitchConnectionHandler, ICoreListener {  
	
	private static final Logger LOG = LoggerFactory.getLogger(ShimSwitchConnectionHandlerImpl.class);
	private static ZeroMQBaseConnector coreConnector;
	private List<ConnectionAdapter> connectionAdapterList; 
	
	public ShimSwitchConnectionHandlerImpl(ZeroMQBaseConnector connector){
		coreConnector = connector;
		connectionAdapterList = new ArrayList<ConnectionAdapter>();
	}
	
	@Override
	public boolean accept(InetAddress arg0) {
		return true;
	}

	@Override
	public void onSwitchConnected(ConnectionAdapter connectionAdapter) {
		LOG.info("SHIM: on Switch connected: " + connectionAdapter.getRemoteAddress().toString());
		connectionAdapterList.add(connectionAdapter);
		ShimMessageListener listener = new ShimMessageListener(coreConnector, connectionAdapter);
		connectionAdapter.setMessageListener(listener);
		connectionAdapter.setSystemListener(listener);
		connectionAdapter.setConnectionReadyListener(listener);
	}

	@Override
	public void onCoreMessage(Message input) {
		// TODO Auto-generated method stub
		if(input instanceof OpenFlowMessage){
			OpenFlowMessage ofm=(OpenFlowMessage)input;
			DataObject message = OFMessageTranslator.translate(ofm);
			
			for (ConnectionAdapter conn : connectionAdapterList){
				conn.flowMod((FlowModInput)message);
			}
			
		}
	}

}


