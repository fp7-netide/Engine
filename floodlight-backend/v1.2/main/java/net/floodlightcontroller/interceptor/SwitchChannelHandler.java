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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class SwitchChannelHandler extends SimpleChannelHandler {

    private DummySwitch dummySwitch;

    ZeroMQBaseConnector coreConnector;

    public void setCoreConnector(ZeroMQBaseConnector connector) {
        coreConnector = connector;
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

    public void sendToCore(OpenFlowMessage msg) {
        coreConnector.SendData(msg.toByteRepresentation());
    }

}
