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

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortMod;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message handler for comms between NetIDE module and ODL-Shim
 *
 * @author aleckey
 *
 */
public class BackendChannelHandler extends SimpleChannelHandler {
	private Map<Long, DummySwitch> pendingSwitches = new HashMap<Long, DummySwitch>();
	private Map<Long, ClientBootstrap> managedBootstraps = new HashMap<Long, ClientBootstrap>();
	private Map<Long, ChannelFuture> managedSwitches = new HashMap<Long, ChannelFuture>();
	private BasicFactory factory;
	private Channel channel;
	protected static Logger logger;
	
	public BackendChannelHandler() {
		logger = LoggerFactory.getLogger(BackendChannelHandler.class);
		factory = new BasicFactory();
	}
	
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		logger.debug("MessageReceived: " );
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        String msg = new String(buf.array());
        logger.debug(msg);
        handleMessage(msg);
        try {
        	List<OFMessage> listMessages = factory.parseMessage(buf);
        	logger.debug(listMessages.get(0).toString());
        } catch(Exception ex) {
        	//ex.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
    
    @Override
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("channelBound: " + e.getChannel().isBound());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("channelConnected: " + e.getChannel().isConnected() + " to: " +
        						e.getChannel().getRemoteAddress());
    	channel = e.getChannel();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("channelClosed: " + e.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("channelDisconnected: " + e.getChannel());
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	logger.debug("channelOpen: " + e.getChannel().isOpen());
    }

    private void handleMessage(String receivedMsg) {
		//POSSIBLE MULTIPLE MESSAGES - SPLIT
		String[] messages = receivedMsg.split("\n");
		for(String msg: messages) {
			switch (getActionType(msg)) {
				case SWITCH :
					//COULD BE SWITCH JOIN(BEGIN/END) OR PART
					MessageSwitch switchMessage = new MessageSwitch(msg);
					//CACHE SWITCH TILL END MESSAGE RECEIVED
					if (switchMessage.getAction().equals("join")) {
						if (switchMessage.isBegin()) {
							DummySwitch newSwitch = new DummySwitch(switchMessage.getId());
							pendingSwitches.put(switchMessage.getId(), newSwitch);
							
						} else {
							//SWITCH END - READY TO ADD DUMMY SWITCH TO CONTROLLER
							DummySwitch existingSwitch = pendingSwitches.get(switchMessage.getId());
							addNewSwitch(existingSwitch);
							pendingSwitches.remove(switchMessage.getId());
						}
					} else {
						//SWITCH PART MSG
						final long switchID = switchMessage.getId();
						ChannelFuture future = managedSwitches.get(switchID);
						future.getChannel().close().addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								managedBootstraps.get(switchID).releaseExternalResources();
								managedBootstraps.remove(switchID);
							}
						});
						managedSwitches.remove(switchID);
					}
					break;
				case PORT :
					//COULD BE PORT JOIN/PART	
					MessagePort portMessage = new MessagePort(msg);
					if (portMessage.getAction().equals("join")) {
						//ADD THE PORT INFO TO ITS SWITCH
						OFPhysicalPort portInfo = portMessage.getOfPort();
						if (pendingSwitches.containsKey(portMessage.getSwitchId())) {
							pendingSwitches.get(portMessage.getSwitchId()).setPort(portInfo);
						} else {
							//SWITCH ALREADY ADDED TO CONTROLLER - NEED TO SEND port_status MESSAGE
							OFPortStatus portStatMsg = (OFPortStatus)factory.getMessage(OFType.PORT_STATUS);
							portStatMsg.setDesc(portInfo);
							portStatMsg.setReason((byte)OFPortStatus.OFPortReason.OFPPR_MODIFY.ordinal());
							sendMessageToController(portMessage.getSwitchId(), portStatMsg);
						}
					} else {
						//PART MSG
						OFPortMod portMod = (OFPortMod) factory.getMessage(OFType.PORT_MOD);
						portMod.setPortNumber(portMessage.getPortNo());
						portMod.setXid(portMessage.getPortNo());
						portMod.setConfig(1); //1: DOWN
						sendMessageToController(portMessage.getSwitchId(), portMod);
					}
					break;
				case PACKET :
					//PARSE INCOMING MESSAGE
					MessagePacket receivedPacket = new MessagePacket(msg);
					OFPacketIn packetIn = receivedPacket.getPacketIn();
					sendMessageToController(receivedPacket.getSwitchId(), packetIn);
					break;
				case FLOW_STATS_REPLY:
					MessageParser mp = new MessageParser();
					OFStatisticsReply OFStatisticsReply = mp.parseStatsReply(OFStatisticsType.FLOW, msg);
					sendMessageToController(mp.getSwitchId(), OFStatisticsReply);
					break;
				case LINK:
					
				default:
					//NOT SUPPORTED YET
			}
		}
    }

	/**Sends an OF message to the SDN Controller from the correct Dummy Switch
	 *  
	 * @param switchId the relevant switch's id
	 * @param message the Openflow message to be sent
	 */
	private void sendMessageToController(long switchId, OFMessage message) {
		//USE THE CORRECT CHANNEL TO SEND MESSAGE
		ChannelFuture future = managedSwitches.get(switchId);
		ChannelBuffer sendData = ChannelBuffers.buffer(message.getLength());
		message.writeTo(sendData);
		future.getChannel().write(sendData);
	}
    
    /**
     * Creates the comms channel to the SDN Controller and then adds a 
     * fake switch for the controller to manage
     * @param dummySwitch the switch to be managed
     */
    private void addNewSwitch(DummySwitch dummySwitch) {
    	final SwitchChannelHandler switchHandler = new SwitchChannelHandler();
    	switchHandler.setDummySwitch(dummySwitch); //CONTAINS ALL THE INFO ABOUT THIS SWITCH
    	switchHandler.setShimChannel(this.channel);
    	
    	ChannelFactory factory = new NioClientSocketChannelFactory(
		                    Executors.newCachedThreadPool(),
		                    Executors.newCachedThreadPool());
        ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                return Channels.pipeline(switchHandler);
            }
        });
        //CONNECT AND ADD TO HASHMAP OF MANAGED SWITCHES
        ChannelFuture future = bootstrap.connect(new InetSocketAddress("localhost", 6634));
        managedSwitches.put(dummySwitch.getId(), future);
        managedBootstraps.put(dummySwitch.getId(), bootstrap);
    }
    
    /**
	 * Assumes the following format: ["switch", "join", 1, "BEGIN"] and would return SWITCH
	 * @param msg
	 * @return
	 */
	private MessageType getActionType(String msg) {
		String tmp = msg.substring(2);
		tmp = tmp.substring(0, tmp.indexOf("\""));
		
		if (tmp.equals("switch")) return MessageType.SWITCH;
		if (tmp.equals("packet")) return MessageType.PACKET;
		if (tmp.equals("port")) return MessageType.PORT;
		
		return MessageType.UNSUPPORTED;
	}
}
