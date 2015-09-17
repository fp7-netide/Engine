package org.opendaylight.netide.shim;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoRequestMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ExperimenterMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowRemovedMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartReplyMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.OpenflowProtocolListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketInMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PortStatusMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.DisconnectEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SwitchIdleEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SystemNotificationsListener;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionReadyListener;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.protocol.impl.deserialization.factories.HelloMessageFactory;
import org.opendaylight.openflowjava.protocol.impl.util.ListSerializer;
import org.opendaylight.openflowjava.util.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPConverter;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;


public class ShimMessageListener implements OpenflowProtocolListener, SystemNotificationsListener, ConnectionReadyListener{
	
	private static final Logger LOG = LoggerFactory.getLogger(ShimMessageListener.class);
	private static ZeroMQBaseConnector coreConnector;
	private ConnectionAdapter switchConnection;
	private static final byte MESSAGE_TYPE = 10;
    private static final byte PADDING_IN_PACKET_OUT_MESSAGE = 6;
	
	public ShimMessageListener(ZeroMQBaseConnector connector, ConnectionAdapter switchConnection){
		this.coreConnector = connector;
		this.switchConnection = switchConnection;
	}
	
	private void sendToCore(byte[] data){
		Message message = new Message(NetIPUtils.StubHeaderFromPayload(data), data);
		message.getHeader().setMessageType(MessageType.OPENFLOW);
		message.getHeader().setDatapathId(0);
		message.getHeader().setModuleId(0);
		message.getHeader().setTransactionId(0);
		coreConnector.SendData(message.toByteRepresentation());
	}
	
	@Override
	public void onEchoRequestMessage(EchoRequestMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
		sendToCore(arg0.getData());
	}

	@Override
	public void onErrorMessage(ErrorMessage arg0) {
		// TODO Auto-generated method stub
		LOG.info("SHIM Message received: " + arg0.toString());
		sendToCore(arg0.getData());
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
		HelloInputBuilder builder = new HelloInputBuilder();
        builder.setVersion((short) EncodeConstants.OF10_VERSION_ID);
        builder.fieldsFrom(arg0);
		switchConnection.hello(builder.build());
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
		ByteBuf out = Unpooled.buffer();
		sendToCore(serialize(arg0, out));
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
	
	public byte[] serialize(PacketInMessage message, ByteBuf outBuffer) {
		ByteBufUtils.writeOFHeader(MESSAGE_TYPE, message, outBuffer, EncodeConstants.EMPTY_LENGTH);
	    outBuffer.writeInt(message.getBufferId().intValue());
	    outBuffer.writeInt(message.getTotalLen().intValue());	        
	    outBuffer.writeInt(message.getInPort().intValue());
	    outBuffer.writeInt(message.getReason().getIntValue());
	    outBuffer.writeZero(PADDING_IN_PACKET_OUT_MESSAGE);
	    byte[] data = message.getData();
	    if (data != null) {
	    	outBuffer.writeBytes(data);
	    }
	    ByteBufUtils.updateOFHeaderLength(outBuffer);
	    return outBuffer.array();        		
	}

}
