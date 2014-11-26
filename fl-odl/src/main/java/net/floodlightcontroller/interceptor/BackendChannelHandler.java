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

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;

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
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
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
	//private Map<Long, SwitchChannelHandler> managedSwitches = new HashMap<Long, SwitchChannelHandler>();
	private Map<Long, ClientBootstrap> managedSwitches = new HashMap<Long, ClientBootstrap>();
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
        handleMessage(msg);
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
			switch (getOFActionType(msg)) {
				case SWITCH :
					//COULD BE SWITCH JOIN(BEGIN/END) OR PART
					OFMessageSwitch switchMessage = new OFMessageSwitch(msg);
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
						final ClientBootstrap bootstrap = managedSwitches.get(switchMessage.getId());
						bootstrap.getPipeline().getChannel().close().addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								bootstrap.releaseExternalResources();
							}
						});
						managedSwitches.remove(switchMessage.getId());
					}
					break;
				case PORT :
					//COULD BE PORT JOIN/PART	
					OFMessagePort portMessage = new OFMessagePort(msg);
					if (portMessage.getAction().equals("join")) {
						//ADD THE PORT INFO TO ITS SWITCH
						OFPhysicalPort portInfo = portMessage.getOfPort();
						pendingSwitches.get(portMessage.getSwitchId()).setPort(portInfo);
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
					OFMessagePacket receivedPacket = new OFMessagePacket(msg);
					OFPacketIn packetIn = receivedPacket.getPacketIn();
					sendMessageToController(receivedPacket.getSwitchId(), packetIn);
					break;
				case FLOW_STATS_REPLY:
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
		ClientBootstrap bootstrap = managedSwitches.get(switchId);
		ChannelBuffer sendData = ChannelBuffers.buffer(message.getLength());
		message.writeTo(sendData);
		bootstrap.getPipeline().getChannel().write(sendData);
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
        bootstrap.connect(new InetSocketAddress("localhost", 6634));
        managedSwitches.put(dummySwitch.getId(), bootstrap);
        //managedSwitches.put(dummySwitch.getId(), switchHandler);
    }
    
    /**
	 * Assumes the following format: ["switch", "join", 1, "BEGIN"] and would return SWITCH
	 * @param msg
	 * @return
	 */
	private OFActionType getOFActionType(String msg) {
		String tmp = msg.substring(2);
		tmp = tmp.substring(0, tmp.indexOf("\""));
		
		if (tmp.equals("switch")) return OFActionType.SWITCH;
		if (tmp.equals("packet")) return OFActionType.PACKET;
		if (tmp.equals("port")) return OFActionType.PORT;
		
		return OFActionType.UNSUPPORTED;
	}
}
