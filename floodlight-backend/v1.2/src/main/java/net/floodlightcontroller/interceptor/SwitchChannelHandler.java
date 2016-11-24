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
	private OFVersion agreedVersion;
	private String moduleName;

	public SwitchChannelHandler(ZeroMQBaseConnector connector, OFVersion agreedVersion, String moduleName) {
		coreConnector = connector;
		logger = LoggerFactory.getLogger(SwitchChannelHandler.class);
		this.agreedVersion = agreedVersion;
		this.moduleName = moduleName;
	}

	public void setModuleHandler(IModuleHandler moduleHandler) {
		this.moduleHandler = moduleHandler;
	}

	public void registerSwitchConnection(ChannelFuture connection) {
		this.future = connection;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		ChannelBuffer buf = (ChannelBuffer) e.getMessage();

		try {
			OFMessage msg = OFFactories.getGenericReader().readFrom(buf);
			handleMessage(msg);
		} catch (OFParseError e1) {
			logger.error(e1.getMessage());
		} catch (IllegalArgumentException e2) {
			logger.error(e2.getMessage());
		}
	}

	private void handleMessage(OFMessage msg) {
		logger.debug("Message received channel handler: " + msg.getType().toString() + " switch datapathID: "
				+ dummySwitch.getDatapathId());

		if (msg.getType().equals(OFType.FLOW_MOD) || msg.getType().equals(OFType.PACKET_OUT)) {
			dummySwitch.setHandshakeCompleted(true);
		}

		if (msg.getType().equals(OFType.HELLO)) {
			Relay.sendToController(future,
					OFFactories.getFactory(agreedVersion).buildHello().setXid(msg.getXid()).build());
		} else if (msg.getType().equals(OFType.FEATURES_REQUEST)) {
			Relay.sendToController(future, dummySwitch.getFeatures());
			dummySwitch.setConnectionHandshake(true);
		} else if (msg.getType().equals(OFType.ECHO_REQUEST)) {
			Relay.sendToController(future, OFFactories.getFactory(msg.getVersion()).buildEchoReply()
					.setXid(msg.getXid()).setData(((OFEchoRequest) msg).getData()).build());
		} else if (msg.getType().equals(OFType.SET_CONFIG)) {
			OFGetConfigRequest.Builder configReqBuilder = OFFactories.getFactory(msg.getVersion())
					.buildGetConfigRequest();
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
		} else if (msg.getType().equals(OFType.ROLE_REQUEST)) {
			OFRoleRequest ofRoleRequest = (OFRoleRequest) msg;
			Relay.sendToController(future, OFFactories.getFactory(msg.getVersion()).buildRoleReply()
					.setXid(msg.getXid()).setRole(ofRoleRequest.getRole()).build());
			dummySwitch.setHandshakeCompleted(true);
		} else {
			Relay.sendToCore(coreConnector, msg, dummySwitch.getDatapathId(),
					moduleHandler.getModuleId(-1, moduleName));
		}
	}

	public void setDummySwitch(DummySwitch _dummySwitch) {
		dummySwitch = _dummySwitch;
	}
}
