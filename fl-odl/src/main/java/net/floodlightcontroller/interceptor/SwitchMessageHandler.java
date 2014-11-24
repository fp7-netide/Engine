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

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFGetConfigReply;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.MessageParseException;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class SwitchMessageHandler extends SimpleChannelHandler {

	//private OFMessageFactory messageFactory;
	private BasicFactory factory; 
	private DummySwitch dummySwitch;
	/**@return the dummySwitch */

	public DummySwitch getDummySwitch() {
		return dummySwitch;
	}

	/**@param dummySwitch the dummySwitch to set */
	public void setDummySwitch(DummySwitch dummySwitch) {
		this.dummySwitch = dummySwitch;
	}
	
	
	public SwitchMessageHandler() {
		//messageFactory = new BasicFactory();
		factory = new BasicFactory();
	}
	
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    	System.out.println("MessageReceived: " );
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        
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
        System.out.println("Bound: " + e.getChannel().isBound());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.out.println("Connected: " + e.getChannel().isConnected());
        System.out.println("Connected: " + e.getChannel().getRemoteAddress());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.out.println("Closed: " + e.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.out.println("Disconnected: " + e.getChannel());
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.out.println("Open: " + e.getChannel().isOpen());
    }
    
    private void handleMessage(List<OFMessage> listMessages, Channel channel) {
		for (OFMessage m : listMessages) {
            // Always handle ECHO REQUESTS, regardless of state
			System.out.println(m.toString());
			ChannelBuffer sendData;
            switch (m.getType()) {
            	case HELLO:
            		System.out.println("Sending Hello...");
            		OFHello hello = (OFHello) factory.getMessage(OFType.HELLO);
            		sendData = ChannelBuffers.buffer(hello.getLength());
            		hello.writeTo(sendData);
//        			helloData.writeByte(hello.getVersion());
//        			helloData.writeByte(hello.getType().getTypeValue());
//        			helloData.writeShort(hello.getLength());
//        			helloData.writeInt(hello.getXid());
            		channel.write(sendData);
            		break;
            	case FEATURES_REQUEST:
            		System.out.println("Sending Features Request...");
            		OFFeaturesReply featuresReply = (OFFeaturesReply) factory.getMessage(OFType.FEATURES_REPLY);
            		featuresReply.setXid(m.getXid());
            		sendData = ChannelBuffers.buffer(featuresReply.getLength());
            		featuresReply.writeTo(sendData);
            		channel.write(sendData);
            		break;
            	case GET_CONFIG_REQUEST:
            		System.out.println("Sending Features Request...");
                	OFGetConfigReply configReply = (OFGetConfigReply) factory.getMessage(OFType.GET_CONFIG_REPLY);
                	configReply.setXid(m.getXid());
                	sendData = ChannelBuffers.buffer(configReply.getLength());
                	configReply.writeTo(sendData);
                	channel.write(sendData);
                	break;
                case STATS_REQUEST:
                	OFStatisticsReply statsReply = (OFStatisticsReply) factory.getMessage(OFType.STATS_REPLY);
                	statsReply.setXid(m.getXid());
                	sendData = ChannelBuffers.buffer(statsReply.getLength());
                	statsReply.writeTo(sendData);
                	channel.write(sendData);
                	break;
                case ECHO_REQUEST:
                    OFEchoReply echo = (OFEchoReply) factory.getMessage(OFType.ECHO_REPLY);
                    echo.setXid(m.getXid());
                    //send(echo, null);
                    break;
                case BARRIER_REQUEST:
                	
                	break;
                case ERROR:
                    //logError(sw, (OFError)m);
                    // fall through intentionally so error can be listened for
                default:
            }
        }
	}

}
