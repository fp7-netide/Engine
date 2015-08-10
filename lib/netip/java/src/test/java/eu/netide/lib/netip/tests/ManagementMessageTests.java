
package eu.netide.lib.netip.tests;

import eu.netide.lib.netip.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Tests for creation, serialization and deserialization of MANAGEMENT messages.
 * Created by timvi on 10.08.2015.
 */
public class ManagementMessageTests {

    /**
     * Test Message 1.
     * <p>
     * Contents:
     * <p>
     * Header:
     * - version: 1.1 (0x02)
     * - type: MANAGEMENT (0x03)
     * - length: 3
     * - xid: 17
     * - module_id: 2
     * - datapath_id: 42
     * <p>
     * Payload:
     * - bla (0x62 0x6c 0x61)
     */
    private static final byte[] expectedMessage1 = new byte[]
            {
                    0x02, 0x03, 0x00, 0x03, 0x00, 0x00, 0x00, 0x11, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2A, 0x62, 0x6c, 0x61
            };

    /**
     * Test message serialization.
     */
    @Test(testName = "ManagementMessage serialization test", suiteName = "ManagementMessage Tests")
    public void TestMessageSerialization() {
        ManagementMessage testMessage = new ManagementMessage();
        testMessage.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        testMessage.getHeader().setPayloadLength((short) 3);
        testMessage.getHeader().setTransactionId(17);
        testMessage.getHeader().setModuleId(2);
        testMessage.getHeader().setDatapathId(42);
        testMessage.setPayloadString("bla");

        byte[] testBytes = testMessage.toByteRepresentation();
        System.out.println("Expected: " + Arrays.toString(expectedMessage1));
        System.out.println("Actual:   " + Arrays.toString(testBytes));
        Assert.assertEquals(testBytes.length, expectedMessage1.length, "Length does not match!");
        Assert.assertEquals(testBytes, expectedMessage1, "Arrays do not match!");
    }

    /**
     * Test general message parsing.
     */
    @Test(testName = "ManagementMessage general parse test", suiteName = "ManagementMessage Tests")
    public void TestMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(testMessage.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(testMessage.getHeader().getMessageType(), MessageType.MANAGEMENT);
        Assert.assertEquals(testMessage.getHeader().getPayloadLength(), 3);
        Assert.assertEquals(testMessage.getHeader().getTransactionId(), 17);
        Assert.assertEquals(testMessage.getHeader().getModuleId(), 2);
        Assert.assertEquals(testMessage.getHeader().getDatapathId(), 42);
        Assert.assertEquals(testMessage.getPayload(), new byte[]{0x62, 0x6c, 0x61});
    }

    /**
     * Test concrete message parsing.
     */
    @Test(testName = "ManagementMessage concrete parse test", suiteName = "ManagementMessage Tests")
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
        Assert.assertEquals(mm.getPayload(), new byte[]{0x62, 0x6c, 0x61});
        Assert.assertEquals(mm.getPayloadString(), "bla");
    }
}
