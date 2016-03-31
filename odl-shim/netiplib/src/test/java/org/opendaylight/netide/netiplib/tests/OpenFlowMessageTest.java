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
import org.opendaylight.netide.netiplib.MessageType;
import org.opendaylight.netide.netiplib.NetIDEProtocolVersion;
import org.opendaylight.netide.netiplib.NetIPConverter;
import org.opendaylight.netide.netiplib.OpenFlowMessage;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.util.ByteBufUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetQueueConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetQueueConfigInputBuilder;

public class OpenFlowMessageTest {
    OpenFlowMessage ofMessage;
    byte[] expectedOfMessage = ByteBufUtils.hexStringToBytes("04 16 00 10 00 00 00 11 00 00 04 d2 00 00 00 00");
    byte[] expectedNetipMessage = ByteBufUtils.hexStringToBytes(
            "02 11 00 10 00 00 00 11 00 00 00 02 00 00 00 00 00 00 00 2A 04 16 00 10 00 00 00 11 00 00 04 d2 00 00 00 00");
    GetQueueConfigInput message;

    @Before
    public void startUp() throws Exception {
        ofMessage = new OpenFlowMessage(EncodeConstants.OF13_VERSION_ID);
        GetQueueConfigInputBuilder builder = new GetQueueConfigInputBuilder();
        builder.setVersion((short) EncodeConstants.OF13_VERSION_ID);
        builder.setXid(17L);
        builder.setPort(new PortNumber(1234L));
        message = builder.build();
        ofMessage.setOfMessage(message);
    }

    @Test
    public void testSerialize() {
        Assert.assertArrayEquals("Wrong payload", expectedOfMessage, ofMessage.getPayload());
    }

    @Test
    public void testMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedNetipMessage);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(NetIDEProtocolVersion.VERSION_1_1, testMessage.getHeader().getNetIDEProtocolVersion());
        Assert.assertEquals(MessageType.OPENFLOW, testMessage.getHeader().getMessageType());
        Assert.assertEquals(16, testMessage.getHeader().getPayloadLength());
        Assert.assertEquals(17, testMessage.getHeader().getTransactionId());
        Assert.assertEquals(2, testMessage.getHeader().getModuleId());
        Assert.assertEquals(42, testMessage.getHeader().getDatapathId());
        Assert.assertArrayEquals(testMessage.getPayload(), expectedOfMessage);
    }

    @Test
    public void testMessageParsingConcrete() {
        Message testMessage = NetIPConverter.parseConcreteMessage(expectedNetipMessage);
        Assert.assertNotNull(testMessage);
        Assert.assertTrue(testMessage instanceof OpenFlowMessage);
        OpenFlowMessage of = (OpenFlowMessage) testMessage;
        Assert.assertEquals(NetIDEProtocolVersion.VERSION_1_1, of.getHeader().getNetIDEProtocolVersion());
        Assert.assertEquals(MessageType.OPENFLOW, of.getHeader().getMessageType());
        Assert.assertEquals(16, of.getHeader().getPayloadLength());
        Assert.assertEquals(17, of.getHeader().getTransactionId());
        Assert.assertEquals(2, of.getHeader().getModuleId());
        Assert.assertEquals(42, of.getHeader().getDatapathId());
        Assert.assertArrayEquals(expectedOfMessage, of.getPayload());
        Assert.assertEquals(message, of.getOfMessage());
    }
}
