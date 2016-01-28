/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib.tests;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.netide.netiplib.ManagementMessage;
import org.opendaylight.netide.netiplib.Message;
import org.opendaylight.netide.netiplib.MessageType;
import org.opendaylight.netide.netiplib.NetIDEProtocolVersion;
import org.opendaylight.netide.netiplib.NetIPConverter;

/**
 * Tests for creation, serialization and deserialization of MANAGEMENT messages.
 * Created by timvi on 10.08.2015.
 */
public class ManagementMessageTest {

    /**
     * Test Message 1.
     * <p>
     * Contents:
     * <p>
     * Header: - version: 1.1 (0x02) - type: MANAGEMENT (0x03) - length: 3 -
     * xid: 17 - module_id: 2 - datapath_id: 42
     * <p>
     * Payload: - bla (0x62 0x6c 0x61)
     */
    private static final byte[] expectedMessage1 = new byte[] { 0x02, 0x03, 0x00, 0x03, 0x00, 0x00, 0x00, 0x11, 0x00,
        0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2A, 0x62, 0x6c, 0x61 };

    /**
     * Test message serialization. testName =
     * "ManagementMessage serialization test", suiteName =
     * "ManagementMessage Tests"
     */
    @Test
    public void TestMessageSerialization() {
        ManagementMessage testMessage = new ManagementMessage();
        testMessage.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        testMessage.getHeader().setPayloadLength((short) 3);
        testMessage.getHeader().setTransactionId(17);
        testMessage.getHeader().setModuleId(2);
        testMessage.getHeader().setDatapathId(42);
        testMessage.setPayloadString("bla");

        byte[] testBytes = testMessage.toByteRepresentation();
        Assert.assertEquals("Length does not match!", testBytes.length, expectedMessage1.length);
        Assert.assertArrayEquals("Arrays do not match!", testBytes, expectedMessage1);
    }

    /**
     * Test general message parsing. testName =
     * "ManagementMessage general parse test", suiteName =
     * "ManagementMessage Tests"
     */
    @Test
    public void TestMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(testMessage.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(testMessage.getHeader().getMessageType(), MessageType.MANAGEMENT);
        Assert.assertEquals(testMessage.getHeader().getPayloadLength(), 3);
        Assert.assertEquals(testMessage.getHeader().getTransactionId(), 17);
        Assert.assertEquals(testMessage.getHeader().getModuleId(), 2);
        Assert.assertEquals(testMessage.getHeader().getDatapathId(), 42);
        Assert.assertArrayEquals(testMessage.getPayload(), new byte[] { 0x62, 0x6c, 0x61 });
    }

    /**
     * Test concrete message parsing. testName =
     * "ManagementMessage concrete parse test", suiteName =
     * "ManagementMessage Tests"
     */
    @Test
    public void TestMessageParsingConcrete() {
        Message testMessage = NetIPConverter.parseConcreteMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertTrue(testMessage instanceof ManagementMessage);
        ManagementMessage mm = (ManagementMessage) testMessage;
        Assert.assertEquals(mm.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(mm.getHeader().getMessageType(), MessageType.MANAGEMENT);
        Assert.assertEquals(mm.getHeader().getPayloadLength(), 3);
        Assert.assertEquals(mm.getHeader().getTransactionId(), 17);
        Assert.assertEquals(mm.getHeader().getModuleId(), 2);
        Assert.assertEquals(mm.getHeader().getDatapathId(), 42);
        Assert.assertArrayEquals(mm.getPayload(), new byte[] { 0x62, 0x6c, 0x61 });
        Assert.assertEquals(mm.getPayloadString(), "bla");
    }
}
