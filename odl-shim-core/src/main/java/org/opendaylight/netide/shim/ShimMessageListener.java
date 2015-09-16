package org.opendaylight.netide.shim;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoRequestMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ExperimenterMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowRemovedMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartReplyMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.OpenflowProtocolListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketInMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PortStatusMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.DisconnectEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SwitchIdleEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SystemNotificationsListener;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionReadyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;


public class ShimMessageListener implements OpenflowProtocolListener, SystemNotificationsListener, ConnectionReadyListener{
	
	private static final Logger LOG = LoggerFactory.getLogger(ShimMessageListener.class);
	private static ZMQ.Socket socket;
	
	public ShimMessageListener(ZMQ.Socket socket){
		this.socket = socket;
	}
	
	
	
	@Override
	public void onEchoRequestMessage(EchoRequestMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
		socket.send(arg0.getData());
	}

	@Override
	public void onErrorMessage(ErrorMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onExperimenterMessage(ExperimenterMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onFlowRemovedMessage(FlowRemovedMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onHelloMessage(HelloMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onMultipartReplyMessage(MultipartReplyMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onPacketInMessage(PacketInMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onPortStatusMessage(PortStatusMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onDisconnectEvent(DisconnectEvent arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onSwitchIdleEvent(SwitchIdleEvent arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
	}

	@Override
	public void onConnectionReady() {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message: ConnectionReady");
	}

}
