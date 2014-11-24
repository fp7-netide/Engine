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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class NetideModuleTester {
	protected static Logger logger;
	
	public static void main(String[] args) {
		logger = LoggerFactory.getLogger(NetideModuleTester.class);
		
        //START UP THE SERVER TO THE ODL-SHIM
        ChannelFactory serverFactory =
            new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool());

        ServerBootstrap serverBootstrap = new ServerBootstrap(serverFactory);
        serverBootstrap.setOption("child.tcpNoDelay", true);
        serverBootstrap.setOption("child.keepAlive", true);
        serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                return Channels.pipeline(new BackendChannelHandler());
            }
        });
        logger.debug("NetIDE Module binding to 41414...");
        serverBootstrap.bind(new InetSocketAddress(41414));
        
		//START UP THE OPENFLOW CLIENT - DUMMY SWITCH
//		ChannelFactory factory =
//	            new NioClientSocketChannelFactory(
//	                    Executors.newCachedThreadPool(),
//	                    Executors.newCachedThreadPool());
//
//        ClientBootstrap bootstrap = new ClientBootstrap(factory);
//        bootstrap.setOption("tcpNoDelay", true);
//        bootstrap.setOption("keepAlive", true);
//        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
//            public ChannelPipeline getPipeline() {
//                return Channels.pipeline(new MessageHandler());
//            }
//        });
//        ChannelFuture conn = bootstrap.connect(new InetSocketAddress("localhost", 6634));
	}
}
