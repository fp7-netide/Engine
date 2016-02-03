package eu.netide.lib.netip.tests;

import eu.netide.lib.netip.*;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Tests for creation, serialization and deserialization of OPENFLOW messages.
 * Created by timvi on 10.08.2015.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 * <p>
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 * <p>
 * Test message serialization.
 * <p>
 * Test general message parsing.
 * <p>
 * Test concrete message parsing.
 *//*

public class OpenFlowMessageTests {

    */
/**
 * Test Message 1.
 * <p>
 * Contents:
 * <p>
 * Header:
 * - version: 1.1 (0x02)
 * - type: OPENFLOW (0x11)
 * - length: 3
 * - xid: 17
 * - module_id: 2
 * - datapath_id: 42
 * <p>
 * Payload:
 * - bla (0x62 0x6c 0x61)
 */

import org.testng.annotations.Test;


public class OpenFlowMessageTests {
    /**
     * Test message serialization.
     */

    private static final byte[] expectedMessage1 = new byte[]
            {
                    0x02, 0x11, 0x00, 0x03, 0x00, 0x00, 0x00, 0x11, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2A, 0x62, 0x6c, 0x61
            };


    @Test(testName = "OpenFlowMessage serialization test", suiteName = "OpenFlowMessage Tests")
    public void TestMessageSerialization() {
        OpenFlowMessage testMessage = new OpenFlowMessage();
        testMessage.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        testMessage.getHeader().setPayloadLength((short) 3);
        testMessage.getHeader().setTransactionId(17);
        testMessage.getHeader().setModuleId(2);
        testMessage.getHeader().setDatapathId(42);
        testMessage.setOfMessage(OFFactories.getFactory(OFVersion.OF_11).buildFlowModify().setMatch(OFFactories.getFactory(OFVersion.OF_11).buildMatch().build()).build());

        byte[] testBytes = testMessage.toByteRepresentation();
        System.out.println("Expected: " + Arrays.toString(expectedMessage1));
        System.out.println("Actual:   " + Arrays.toString(testBytes));
        Assert.assertEquals(testBytes.length, expectedMessage1.length, "Length does not match!");
        Assert.assertEquals(testBytes, expectedMessage1, "Arrays do not match!");
    }


    /**
     * Test general message parsing.
     */

    @Test(testName = "OpenFlowMessage general parse test", suiteName = "OpenFlowMessage Tests")
    public void TestMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(testMessage.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(testMessage.getHeader().getMessageType(), MessageType.OPENFLOW);
        Assert.assertEquals(testMessage.getHeader().getPayloadLength(), 3);
        Assert.assertEquals(testMessage.getHeader().getTransactionId(), 17);
        Assert.assertEquals(testMessage.getHeader().getModuleId(), 2);
        Assert.assertEquals(testMessage.getHeader().getDatapathId(), 42);
        Assert.assertEquals(testMessage.getPayload(), new byte[]{0x62, 0x6c, 0x61});
    }


    /**
     * Test concrete message parsing.
     */

    @Test(testName = "OpenFlowMessage concrete parse test", suiteName = "OpenFlowMessage Tests")
    public void TestMessageParsingConcrete() {
        Message testMessage = NetIPConverter.parseConcreteMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertTrue(testMessage instanceof OpenFlowMessage);
        OpenFlowMessage mm = (OpenFlowMessage) testMessage;
        Assert.assertEquals(mm.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(mm.getHeader().getMessageType(), MessageType.OPENFLOW);
        Assert.assertEquals(mm.getHeader().getPayloadLength(), 3);
        Assert.assertEquals(mm.getHeader().getTransactionId(), 17);
        Assert.assertEquals(mm.getHeader().getModuleId(), 2);
        Assert.assertEquals(mm.getHeader().getDatapathId(), 42);
        Assert.assertEquals(mm.getPayload(), new byte[]{0x62, 0x6c, 0x61});
    }
}
