/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.netide.netiplib.Message;
import org.opendaylight.netide.netiplib.MessageHeader;
import org.opendaylight.netide.netiplib.MessageType;
import org.opendaylight.netide.netiplib.NetIDEProtocolVersion;
import org.opendaylight.netide.netiplib.NetIPConverter;
import org.opendaylight.openflowjava.util.ByteBufUtils;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class MessageTest {
    Message message;
    byte[] payload = ByteBufUtils.hexStringToBytes("04 16 00 10 00 00 00 11 00 00 04 d2 00 00 00 00");
    byte[] expectedMessage = ByteBufUtils.hexStringToBytes(
            "02 11 00 10 00 00 00 11 00 00 00 02 00 00 00 00 00 00 00 2A 04 16 00 10 00 00 00 11 00 00 04 d2 00 00 00 00");

    @Before
    public void startUp() throws Exception {
        MessageHeader header = new MessageHeader();
        header.setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        header.setMessageType(MessageType.OPENFLOW);
        header.setPayloadLength((short) 16);
        header.setTransactionId(17);
        header.setModuleId(2);
        header.setDatapathId(42L);
        message = new Message(header, payload);
    }

    @Test
    public void testSerialization() {
        Assert.assertArrayEquals("Wrong byte representation", expectedMessage, message.toByteRepresentation());
    }

    @Test
    public void testMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedMessage);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(testMessage.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(testMessage.getHeader().getMessageType(), MessageType.OPENFLOW);
        Assert.assertEquals(testMessage.getHeader().getPayloadLength(), 16);
        Assert.assertEquals(testMessage.getHeader().getTransactionId(), 17);
        Assert.assertEquals(testMessage.getHeader().getModuleId(), 2);
        Assert.assertEquals(testMessage.getHeader().getDatapathId(), 42);
        Assert.assertArrayEquals(testMessage.getPayload(), payload);
    }

}
