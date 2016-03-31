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
import org.opendaylight.netide.netiplib.OpFlexMessage;
import org.opendaylight.openflowjava.util.ByteBufUtils;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class OpFlexMessageTest {
    OpFlexMessage message;
    byte[] payload = ByteBufUtils.hexStringToBytes("70 61 79 6C 6F 61 64");
    byte[] expectedMessage = ByteBufUtils
            .hexStringToBytes("02 13 00 07 00 00 00 11 00 00 00 02 00 00 00 00 00 00 00 2A 70 61 79 6C 6F 61 64");

    @Before
    public void startUp() throws Exception {
        MessageHeader header = new MessageHeader();
        header.setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        header.setMessageType(MessageType.OPFLEX);
        header.setPayloadLength((short) 7);
        header.setTransactionId(17);
        header.setModuleId(2);
        header.setDatapathId(42L);
        message = new OpFlexMessage();
        message.setHeader(header);
        message.setPayload(payload);
    }

    @Test
    public void testSerialization() {
        Assert.assertArrayEquals("Wrong byte representation", expectedMessage, message.toByteRepresentation());
    }

    @Test
    public void testMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedMessage);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(NetIDEProtocolVersion.VERSION_1_1, testMessage.getHeader().getNetIDEProtocolVersion());
        Assert.assertEquals(MessageType.OPFLEX, testMessage.getHeader().getMessageType());
        Assert.assertEquals(7, testMessage.getHeader().getPayloadLength());
        Assert.assertEquals(17, testMessage.getHeader().getTransactionId());
        Assert.assertEquals(2, testMessage.getHeader().getModuleId());
        Assert.assertEquals(42, testMessage.getHeader().getDatapathId());
        Assert.assertArrayEquals(testMessage.getPayload(), payload);
    }

    @Test
    public void testMessageParsingConcrete() {
        Message testMessage = NetIPConverter.parseConcreteMessage(expectedMessage);
        Assert.assertNotNull(testMessage);
        Assert.assertTrue(testMessage instanceof OpFlexMessage);
        OpFlexMessage netconfMessage = (OpFlexMessage) testMessage;
        Assert.assertEquals(NetIDEProtocolVersion.VERSION_1_1, netconfMessage.getHeader().getNetIDEProtocolVersion());
        Assert.assertEquals(MessageType.OPFLEX, netconfMessage.getHeader().getMessageType());
        Assert.assertEquals(7, netconfMessage.getHeader().getPayloadLength());
        Assert.assertEquals(17, netconfMessage.getHeader().getTransactionId());
        Assert.assertEquals(2, netconfMessage.getHeader().getModuleId());
        Assert.assertEquals(42, netconfMessage.getHeader().getDatapathId());
        Assert.assertArrayEquals(payload, netconfMessage.getPayload());
    }
}
