/**
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *     ...
 */
package net.floodlightcontroller.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFGetConfigReply;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.MessageParseException;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message handler for comms between Floodlight and Switch
 *
 * @author aleckey
 *
 */
public class SwitchChannelHandler extends SimpleChannelHandler {

	//private OFMessageFactory messageFactory;
	private BasicFactory factory; 
	private DummySwitch dummySwitch;
	private ChannelFuture future;
	protected static Logger logger;
	private Channel shimChannel;
	
	/**@return the dummySwitch */
	public DummySwitch getDummySwitch() {
		return dummySwitch;
	}

	/**@param dummySwitch the dummySwitch to set */
	public void setDummySwitch(DummySwitch dummySwitch) {
		this.dummySwitch = dummySwitch;
	}
	
	public void setControllerChannel(ChannelFuture channel) {
		future = channel;
	}
	
	/**Set the channel used for comms to Shim
	 * @param channel
	 */
	public void setShimChannel(Channel shimChannel) {
		this.shimChannel = shimChannel;
	}
	
	public SwitchChannelHandler() {
		logger = LoggerFactory.getLogger(SwitchChannelHandler.class);
		factory = new BasicFactory();
	}
	
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    	logger.debug("MessageReceived: " );
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        //String msg = new String(buf.array());
        //logger.debug(msg);
		try {
			List<OFMessage> listMessages = factory.parseMessage(buf);
			handleMessage(listMessages, e.getChannel());
		} catch (MessageParseException ex) {
			ex.printStackTrace();
		}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
    
    @Override
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("Bound: " + e.getChannel().isBound());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("Connected: " + e.getChannel().isConnected());
    	logger.debug("Connected: " + e.getChannel().getRemoteAddress());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("Closed: " + e.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("Disconnected: " + e.getChannel());
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("Open: " + e.getChannel().isOpen());
    }
    
    private void sendMessageToController(OFMessage message) {
		//USE THE CORRECT CHANNEL TO SEND MESSAGE
		ChannelBuffer sendData = ChannelBuffers.buffer(message.getLength());
		message.writeTo(sendData);
		future.getChannel().write(sendData);
	}
    
    private void handleMessage(List<OFMessage> listMessages, Channel channel) {
		for (OFMessage m : listMessages) {
			logger.debug(m.toString());
			ChannelBuffer sendData;
            switch (m.getType()) {
	            case ECHO_REQUEST:
	            	//logger.debug("Sending Echo Request to controller");
	                //sendMessageToController(m);
	            	logger.debug("Sending Echo reply...");
	                OFEchoReply echo = (OFEchoReply) factory.getMessage(OFType.ECHO_REPLY);
	                echo.setXid(m.getXid());
	                sendData = ChannelBuffers.buffer(echo.getLength());
	                echo.writeTo(sendData);
	            	channel.write(sendData);
	                break;
            	case HELLO:
            		//logger.debug("Sending Hello to controller");
            		//sendMessageToController(m);
            		logger.debug("Sending Hello reply...");
            		OFHello hello = (OFHello) factory.getMessage(OFType.HELLO);
            		sendData = ChannelBuffers.buffer(hello.getLength());
            		hello.writeTo(sendData);
            		channel.write(sendData);
            		break;
            	case FEATURES_REQUEST:
            		//logger.debug("Sending Features Request to controller");
            		//System.out.println(m.toString());
            		//sendMessageToController(m);
            		logger.debug("Sending Features reply...");
            		OFFeaturesReply featuresReply = (OFFeaturesReply) factory.getMessage(OFType.FEATURES_REPLY);
            		featuresReply.setXid(m.getXid());
            		featuresReply.setDatapathId(dummySwitch.getId());
            		featuresReply.setPorts(new ArrayList<OFPhysicalPort>(dummySwitch.getPorts()));
            		sendData = ChannelBuffers.buffer(featuresReply.getLength());
            		featuresReply.writeTo(sendData);
            		channel.write(sendData);
            		break;
            	case GET_CONFIG_REQUEST:
            		logger.debug("Sending Config reply...");
                	OFGetConfigReply configReply = (OFGetConfigReply) factory.getMessage(OFType.GET_CONFIG_REPLY);
                	configReply.setXid(m.getXid());
                	configReply.setMissSendLength((short)0xffff);
                	sendData = ChannelBuffers.buffer(configReply.getLength());
                	configReply.writeTo(sendData);
                	channel.write(sendData);
                	break;
                case STATS_REQUEST:
                	OFStatisticsRequest statsRequest = (OFStatisticsRequest) m;
                	if (statsRequest.getStatisticType().equals(OFStatisticsType.DESC)) {
                		//TODO: EVENTUALLY SEND desc TO SHIM FOR REPLY INSTEAD OF HANDLING LOCALLY
                		logger.debug("Sending Stats reply...");
                    	OFStatisticsReply statsReply = (OFStatisticsReply) factory.getMessage(OFType.STATS_REPLY);
                    	statsReply.setXid(m.getXid());
                    	statsReply.setStatisticType(OFStatisticsType.DESC);
    					statsReply.setStatisticsFactory(factory);				
    					List<OFStatistics> statistics = new ArrayList<OFStatistics>();
    					OFDescriptionStatistics description = new OFDescriptionStatistics();
    					description.setDatapathDescription("A");
    					description.setHardwareDescription("B");
    					description.setManufacturerDescription("C");
    					description.setSerialNumber("D");
    					description.setSoftwareDescription("E");
    					statistics.add(description);
    					statsReply.setStatistics(statistics);
    					statsReply.setLength((short) 1068);
                    	
    					sendData = ChannelBuffers.buffer(statsReply.getLength());
                    	statsReply.writeTo(sendData);
                    	channel.write(sendData);
                	} else {
                		logger.debug("Sending Stats request to shim: " + statsRequest.getStatisticType().toString());
                		sendMessageToShim(m);
                	}
                	break;
                case FLOW_MOD:
                	sendMessageToShim(m);
                	break;
                case PACKET_OUT: 
                	sendMessageToShim(m);
                	break;
                	
                case ERROR:
                    //logError(sw, (OFError)m);
                    // fall through intentionally so error can be listened for
                	break;
                default:
                	System.out.println("Other type of message received");
                	System.out.println("message is "+ m.toString()) ;
            }
        }
	}

    /**Sends a message to Shim for action
	 *  
	 * @param switchId the relevant switch's id
	 * @param message the Openflow message to be sent
	 */
	private void sendMessageToShim(OFMessage message) {
		logger.debug("Sending message to Shim: " + OFMessage.getDataAsString(dummySwitch, message, null));
		byte[] bMessage = serializeMessage(message);
		this.shimChannel.write(ChannelBuffers.copiedBuffer(bMessage));
	}
	
	private byte[] serializeMessage(OFMessage message) {
		String serializedMsg = "";
		if (message instanceof OFPacketOut) {
			OFPacketOut packetOut = (OFPacketOut)message;
			serializedMsg = MessageSerializer.serializeMessage(this.dummySwitch.getId(), packetOut);
		}
		else if (message instanceof OFFlowMod) {
			OFFlowMod flowMod = (OFFlowMod)message;
			serializedMsg = MessageSerializer.serializeMessage(this.dummySwitch.getId(), flowMod);
		} 
		else if (message instanceof OFStatisticsRequest) {
			OFStatisticsRequest statRequest = (OFStatisticsRequest)message;			
			serializedMsg = MessageSerializer.serializeMessage(this.dummySwitch.getId(), statRequest);
		}
		logger.debug("serialized: " + serializedMsg);
		return serializedMsg.getBytes();
	}
}
