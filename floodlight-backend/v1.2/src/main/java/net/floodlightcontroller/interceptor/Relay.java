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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class Relay {

    protected static Logger logger = LoggerFactory.getLogger(NetIdeModule.class);;

    public static void sendToCore(ZeroMQBaseConnector coreConnector, OFMessage msg, long datapathId, int moduleId) {
        OpenFlowMessage ofMessage = new OpenFlowMessage();
        ofMessage.getHeader().setDatapathId(datapathId);
        ofMessage.getHeader().setTransactionId((int) msg.getXid());
        ofMessage.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_2);
        ofMessage.getHeader().setModuleId(moduleId);
        ChannelBuffer dcb = ChannelBuffers.dynamicBuffer();
        msg.writeTo(dcb);
        byte[] payload = new byte[dcb.readableBytes()];
        ofMessage.getHeader().setPayloadLength((short) payload.length);
        ofMessage.setOfMessage(msg);
        coreConnector.SendData(ofMessage.toByteRepresentation());
    }
}
