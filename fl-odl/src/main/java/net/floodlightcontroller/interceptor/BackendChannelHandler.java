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
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.openflow.protocol.OFPhysicalPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class BackendChannelHandler extends SimpleChannelHandler {
	private Map<Long, DummySwitch> pendingSwitches = new HashMap<Long, DummySwitch>();
	private Map<Long, SwitchChannelHandler> managedSwitches = new HashMap<Long, SwitchChannelHandler>();
	protected static Logger logger;
	
	public BackendChannelHandler() {
		logger = LoggerFactory.getLogger(BackendChannelHandler.class);
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
					//COULD BE SWITCH BEGIN/END/PART
					OFMessageSwitch switchMessage = new OFMessageSwitch(msg);
					//CACHE SWITCH TILL END MESSAGE RECEIVED
					if (switchMessage.getAction().equals("join")) {
						if (switchMessage.isBegin()) {
							DummySwitch newSwitch = new DummySwitch(switchMessage.getId());
							pendingSwitches.put(switchMessage.getId(), newSwitch);
							
						} else {
							//READY TO ADD DUMMY SWITCH TO FLOODLIGHT
							DummySwitch existingSwitch = pendingSwitches.get(switchMessage.getId());
							addNewSwitch(existingSwitch);
							pendingSwitches.remove(switchMessage.getId());
						}
					} else {
						//SWITCH.PART MSG
						//backendChannel.removeSwitch(existingSwitch);
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
						SwitchChannelHandler switchHandler = managedSwitches.get(portMessage.getSwitchId());
						//switchHandler...
					}
					break;
				case PACKET :
					
					break;
				default:
					//NOT SUPPORTED YET
			}
		}
    }
    
    /**
     * Creates the comms channel to floodlight and then adds a 
     * fake switch for Floodlight to manage
     * @param dummySwitch the switch to be managed
     */
    private void addNewSwitch(DummySwitch dummySwitch) {
    	final SwitchChannelHandler switchHandler = new SwitchChannelHandler();
    	switchHandler.setDummySwitch(dummySwitch); //CONTAINS ALL THE INFO ABOUT THIS SWITCH
    	
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
        managedSwitches.put(dummySwitch.getId(), switchHandler);
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
