/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import eu.netide.lib.netip.NetIDEProtocolVersion;
import eu.netide.lib.netip.OpenFlowMessage;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFGetConfigRequest.Builder;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFNiciraControllerRoleRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void setModuleHandler(IModuleHandler moduleHandler) {
        this.moduleHandler = moduleHandler;
    }

    public void registerSwitchConnection(ChannelFuture connection, ClientBootstrap bootstrap) {
        future = connection;
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
            OpenFlowMessage ofMessage = new OpenFlowMessage();
            ofMessage.getHeader().setDatapathId(dummySwitch.getDatapathId());
            ofMessage.getHeader().setTransactionId((int) msg.getXid());
            ofMessage.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_2);

            // TODO Set to the correct ModuleID
            ofMessage.getHeader().setModuleId(-1);

            // ofMessage.getHeader()
            // .setModuleId(moduleHandler.getModuleId((int) msg.getXid(),
            // e.getRemoteAddress().toString()));

            ofMessage.setOfMessage(msg);
            sendToCore(ofMessage);
        } catch (OFParseError e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void setDummySwitch(DummySwitch _dummySwitch) {
        dummySwitch = _dummySwitch;
    }

    private void sendToCore(OpenFlowMessage msg) {
        if (msg.getOfMessage().getType().equals(OFType.HELLO)) {
            msg.getHeader().setTransactionId(msg.getHeader().getTransactionId());
            logger.debug("Sending Automatic Hello Message to Controller " + msg.getOfMessage().getType().toString());
            sendMessageToController(
                    OFFactories.getFactory(agreedVersion).buildHello().setXid(msg.getOfMessage().getXid()).build());
        } else if (msg.getOfMessage().getType().equals(OFType.SET_CONFIG)) {
            Builder configReqBuilder = OFFactories.getFactory(msg.getOfMessage().getVersion()).buildGetConfigRequest();
            configReqBuilder.setXid(msg.getOfMessage().getXid());
            OpenFlowMessage getConfig = new OpenFlowMessage();
            getConfig.setOfMessage(configReqBuilder.build());
            getConfig.setHeader(msg.getHeader());
            coreConnector.SendData(getConfig.toByteRepresentation());
            coreConnector.SendData(msg.toByteRepresentation());

        } else if (msg.getOfMessage().getType().equals(OFType.FLOW_MOD)) {
            logger.debug("Ignoring FlowMod " + msg.getOfMessage().getType().toString());

        } else if (msg.getOfMessage().getType().equals(OFType.FEATURES_REQUEST)) {
            sendMessageToController(dummySwitch.getFeatures());

        } else if (msg.getOfMessage().getType().equals(OFType.EXPERIMENTER)) {
            OFExperimenter experimenter = (OFExperimenter) msg.getOfMessage();
            if (experimenter.getExperimenter() == 8992) {
                OFNiciraControllerRoleRequest roleRequest = (OFNiciraControllerRoleRequest) experimenter;

                sendMessageToController(
                        OFFactories.getFactory(msg.getOfMessage().getVersion()).buildNiciraControllerRoleReply()
                                .setXid(roleRequest.getXid()).setRole(roleRequest.getRole()).build());
            }

        } else {
            logger.debug("Sending OF Message to Core " + msg.getOfMessage().getType().toString());
            coreConnector.SendData(msg.toByteRepresentation());
        }
    }

    private void sendMessageToController(OFMessage message) {
        // USE THE CORRECT CHANNEL TO SEND MESSAGE
        ChannelBuffer dcb = ChannelBuffers.dynamicBuffer();
        try {
            message.writeTo(dcb);
            future.getChannel().write(dcb);
        } catch (Exception e) {

        }
    }

    /*
     * @Override public void exceptionCaught(ChannelHandlerContext ctx,
     * ExceptionEvent e) throws Exception { try { logger.debug(
     * "ERROR: Unhandled exception: " + e.getCause().getMessage() +
     * ". Closing channel " + ctx.getChannel().getId()); } catch (Exception ex)
     * { logger.debug(
     * "ERROR trying to close socket because we got an unhandled exception"); }
     * }
     */
}
