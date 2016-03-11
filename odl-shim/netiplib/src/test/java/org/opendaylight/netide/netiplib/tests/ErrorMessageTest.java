/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib.tests;

import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.netide.netiplib.ErrorMessage;
import org.opendaylight.netide.netiplib.Message;
import org.opendaylight.netide.netiplib.MessageType;
import org.opendaylight.netide.netiplib.NetIDEProtocolVersion;
import org.opendaylight.netide.netiplib.NetIPConverter;
import org.opendaylight.netide.netiplib.Protocol;
import org.opendaylight.netide.netiplib.ProtocolVersions;

/**
 * Tests for creation, serialization and deserialization of ERROR messages.
 * Created by timvi on 10.08.2015.
 */
public class ErrorMessageTest {

    private static final byte[] expectedMessage1 = new byte[] { 0x02, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x11, 0x00,
        0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2A, 0x11, 0x02, 0x12, 0x01 };

    /**
     * Test message serialization. testName = "ErrorMessage serialization test",
     * suiteName = "ErrorMessage Tests"
     */
    @Test
    public void TestMessageSerialization() {
        ErrorMessage testMessage = new ErrorMessage();
        testMessage.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        testMessage.getHeader().setPayloadLength((short) 4);
        testMessage.getHeader().setTransactionId(17);
        testMessage.getHeader().setModuleId(2);
        testMessage.getHeader().setDatapathId(42);
        testMessage.getSupportedProtocols().add(new Pair<>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_1));
        testMessage.getSupportedProtocols().add(new Pair<>(Protocol.NETCONF, ProtocolVersions.NETCONF_1_0));
        byte[] testBytes = testMessage.toByteRepresentation();
        Assert.assertEquals("Length does not match!", testBytes.length, expectedMessage1.length);
        Assert.assertArrayEquals("Arrays do not match!", testBytes, expectedMessage1);
    }

    /**
     * Test general message parsing. testName =
     * "ErrorMessage general parse test", suiteName = "ErrorMessage Tests"
     */
    @Test
    public void TestMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(testMessage.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(testMessage.getHeader().getMessageType(), MessageType.ERROR);
        Assert.assertEquals(testMessage.getHeader().getPayloadLength(), 4);
        Assert.assertEquals(testMessage.getHeader().getTransactionId(), 17);
        Assert.assertEquals(testMessage.getHeader().getModuleId(), 2);
        Assert.assertEquals(testMessage.getHeader().getDatapathId(), 42);
        Assert.assertArrayEquals(testMessage.getPayload(), new byte[] { 0x11, 0x02, 0x12, 0x01 });
    }

    /**
     * Test concrete message parsing. testName =
     * "ErrorMessage concrete parse test", suiteName = "ErrorMessage Tests"
     */
    @Test
    public void TestMessageParsingConcrete() {
        Message testMessage = NetIPConverter.parseConcreteMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertTrue(testMessage instanceof ErrorMessage);
        ErrorMessage em = (ErrorMessage) testMessage;
        Assert.assertEquals(em.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(em.getHeader().getMessageType(), MessageType.ERROR);
        Assert.assertEquals(em.getHeader().getPayloadLength(), 4);
        Assert.assertEquals(em.getHeader().getTransactionId(), 17);
        Assert.assertEquals(em.getHeader().getModuleId(), 2);
        Assert.assertEquals(em.getHeader().getDatapathId(), 42);
        Assert.assertArrayEquals(em.getPayload(), new byte[] { 0x11, 0x02, 0x12, 0x01 });
        Assert.assertEquals(em.getSupportedProtocols().get(0).getValue0(), Protocol.OPENFLOW);
        Assert.assertEquals(em.getSupportedProtocols().get(0).getValue1(), ProtocolVersions.OPENFLOW_1_1);
        Assert.assertEquals(em.getSupportedProtocols().get(1).getValue0(), Protocol.NETCONF);
        Assert.assertEquals(em.getSupportedProtocols().get(1).getValue1(), ProtocolVersions.NETCONF_1_0);
    }
}
