
package eu.netide.lib.netip.tests;

import eu.netide.lib.netip.*;
import org.javatuples.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Tests for creation, serialization and deserialization of ERROR messages.
 * Created by timvi on 10.08.2015.
 */
public class ErrorMessageTests {

    /**
     * Test Message 1.
     * <p>
     * Contents:
     * <p>
     * Header:
     * - version: 1.1 (0x02)
     * - type: ERROR (0x02)
     * - length: 4
     * - xid: 17
     * - module_id: 2
     * - datapath_id: 42
     * <p>
     * Payload:
     * - OpenFlow:1.1 (0x11, 0x02)
     * - NetConf:1.0 (0x12, 0x01)
     */
    private static final byte[] expectedMessage1 = new byte[]
            {
                    0x02, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x11, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2A, 0x11, 0x02, 0x12, 0x01
            };

    /**
     * Test message serialization.
     */
    @Test(testName = "ErrorMessage serialization test", suiteName = "ErrorMessage Tests")
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
        System.out.println("Expected: " + Arrays.toString(expectedMessage1));
        System.out.println("Actual:   " + Arrays.toString(testBytes));
        Assert.assertEquals(testBytes.length, expectedMessage1.length, "Length does not match!");
        Assert.assertEquals(testBytes, expectedMessage1, "Arrays do not match!");
    }

    /**
     * Test general message parsing.
     */
    @Test(testName = "ErrorMessage general parse test", suiteName = "ErrorMessage Tests")
    public void TestMessageParsingGeneral() {
        Message testMessage = NetIPConverter.parseRawMessage(expectedMessage1);
        Assert.assertNotNull(testMessage);
        Assert.assertEquals(testMessage.getHeader().getNetIDEProtocolVersion(), NetIDEProtocolVersion.VERSION_1_1);
        Assert.assertEquals(testMessage.getHeader().getMessageType(), MessageType.ERROR);
        Assert.assertEquals(testMessage.getHeader().getPayloadLength(), 4);
        Assert.assertEquals(testMessage.getHeader().getTransactionId(), 17);
        Assert.assertEquals(testMessage.getHeader().getModuleId(), 2);
        Assert.assertEquals(testMessage.getHeader().getDatapathId(), 42);
        Assert.assertEquals(testMessage.getPayload(), new byte[]{0x11, 0x02, 0x12, 0x01});
    }

    /**
     * Test concrete message parsing.
     */
    @Test(testName = "ErrorMessage concrete parse test", suiteName = "ErrorMessage Tests")
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
        Assert.assertEquals(em.getPayload(), new byte[]{0x11, 0x02, 0x12, 0x01});
        Assert.assertEquals(em.getSupportedProtocols().get(0).getValue0(), Protocol.OPENFLOW);
        Assert.assertEquals(em.getSupportedProtocols().get(0).getValue1(), ProtocolVersions.OPENFLOW_1_1);
        Assert.assertEquals(em.getSupportedProtocols().get(1).getValue0(), Protocol.NETCONF);
        Assert.assertEquals(em.getSupportedProtocols().get(1).getValue1(), ProtocolVersions.NETCONF_1_0);
    }
}
