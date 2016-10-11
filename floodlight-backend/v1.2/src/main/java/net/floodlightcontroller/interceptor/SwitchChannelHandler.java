/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRoleRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netide.lib.netip.NetIDEProtocolVersion;
import net.floodlightcontroller.core.IFloodlightProviderService;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class SwitchChannelHandler extends SimpleChannelHandler {

    private DummySwitch dummySwitch;
    private IModuleHandler moduleHandler;
    ZeroMQBaseConnector coreConnector;
    protected static Logger logger;
    private ChannelFuture future;
    private ClientBootstrap bootstrap;
    private OFVersion agreedVersion;
    private final String moduleName = "floodlight-backend";
    private IFloodlightProviderService floodlightProvider;
    private NetIDEProtocolVersion netIpVersion;
    
    public void setNetIDEProtocolVersion (NetIDEProtocolVersion netIpVersion) {
        this.netIpVersion = netIpVersion;
    }
    
    public void setFloodlightProviderService (IFloodlightProviderService floodlightProvider) {
        this.floodlightProvider = floodlightProvider;
    }
    
    public void setModuleHandler(IModuleHandler moduleHandler) {
        this.moduleHandler = moduleHandler;
    }

    public void registerSwitchConnection(ChannelFuture connection, ClientBootstrap bootstrap) {
        this.future = connection;
        this.bootstrap = bootstrap;
    }

    public SwitchChannelHandler(ZeroMQBaseConnector connector, OFVersion agreedVersion) {
        coreConnector = connector;
        logger = LoggerFactory.getLogger(SwitchChannelHandler.class);
        this.agreedVersion = agreedVersion;

    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();

        try {
            OFMessage msg = OFFactories.getGenericReader().readFrom(buf);
            /*logger.debug("Message received channel handler: " + msg.getType().toString() + " switch datapathID: " + dummySwitch.getDatapathId());*/
            handleMessage(msg);
        } catch (OFParseError e1) {
            // TODO Auto-generated catch block
            logger.error(e1.getMessage());
        } catch (IllegalArgumentException e2) {
            // TODO Auto-generated catch block
            logger.error(e2.getMessage());
        }
    }

    private void handleMessage(OFMessage msg) {
    	
    	if (dummySwitch.nextXid == -1 && 
    			!msg.getType().equals(OFType.HELLO) && 
    			!msg.getType().equals(OFType.EXPERIMENTER) && 
    			!msg.getType().equals(OFType.FEATURES_REQUEST) && 
    			!msg.getType().equals(OFType.ECHO_REQUEST))
    		dummySwitch.nextXid = msg.getXid();
    	else 
    		dummySwitch.nextXid = -1;
    		
    	
        if (msg.getType().equals(OFType.HELLO)) {
            Relay.sendToController(future,
                    OFFactories.getFactory(agreedVersion).buildHello().setXid(msg.getXid()).build());
        } else if (msg.getType().equals(OFType.FEATURES_REQUEST)) {
            Relay.sendToController(future, dummySwitch.getFeatures());
        } else if (msg.getType().equals(OFType.SET_CONFIG)) {
            OFGetConfigRequest.Builder configReqBuilder = OFFactories.getFactory(msg.getVersion()).buildGetConfigRequest();
            configReqBuilder.setXid(msg.getXid());
            Relay.sendToCore(coreConnector, configReqBuilder.build(), dummySwitch.getDatapathId(),
                    moduleHandler.getModuleId(-1, moduleName));
            Relay.sendToCore(coreConnector, msg, dummySwitch.getDatapathId(),
                    moduleHandler.getModuleId(-1, moduleName));
        } else if (msg.getType().equals(OFType.EXPERIMENTER)) {
            OFExperimenter experimenter = (OFExperimenter) msg;
            if (experimenter.getExperimenter() == 8992) {
                OFNiciraControllerRoleRequest roleRequest = (OFNiciraControllerRoleRequest) experimenter;
                Relay.sendToController(future, OFFactories.getFactory(msg.getVersion()).buildNiciraControllerRoleReply()
                        .setXid(roleRequest.getXid()).setRole(roleRequest.getRole()).build());
                dummySwitch.setHandshakeCompleted(true);
            }
        } else if (msg.getType().equals(OFType.FLOW_MOD) || msg.getType().equals(OFType.PACKET_OUT)) {
            dummySwitch.setHandshakeCompleted(true);
            Relay.sendToCore(coreConnector, msg, dummySwitch.getDatapathId(),
                    moduleHandler.getModuleId(-1, moduleName));
        } else if (msg.getType().equals(OFType.ECHO_REQUEST)) {
        	Relay.sendToController(future, OFFactories.getFactory(msg.getVersion()).buildEchoReply()
                    .setXid(msg.getXid()).setData(((OFEchoRequest) msg).getData()).build());
        } else {
        	logger.debug("Message received channel handler: " + msg.getType().toString() + " switch datapathID: " + dummySwitch.getDatapathId());
            Relay.sendToCore(coreConnector, msg, dummySwitch.getDatapathId(),
                    moduleHandler.getModuleId(-1, moduleName));
        }
    }

    public void setDummySwitch(DummySwitch _dummySwitch) {
        dummySwitch = _dummySwitch;
    }
}
