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
import org.opendaylight.netide.netiplib.MessageHeader;
import org.opendaylight.netide.netiplib.MessageType;
import org.opendaylight.netide.netiplib.NetIDEProtocolVersion;
import org.opendaylight.netide.netiplib.NetIPConverter;
import org.opendaylight.openflowjava.util.ByteBufUtils;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class MessageHeaderTest {
    MessageHeader header;
    byte[] expectedHeader = ByteBufUtils
            .hexStringToBytes("02 11 00 10 00 00 00 11 00 00 00 02 00 00 00 00 00 00 00 2A");

    @Before
    public void startUp() throws Exception {
        header = new MessageHeader();
        header.setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_1);
        header.setMessageType(MessageType.OPENFLOW);
        header.setPayloadLength((short) 16);
        header.setTransactionId(17);
        header.setModuleId(2);
        header.setDatapathId(42L);
    }

    @Test
    public void testSerialization() {
        Assert.assertArrayEquals("Wrong byte representation", expectedHeader, header.toByteRepresentation());
    }

    @Test
    public void testParsing() {
        MessageHeader output = NetIPConverter.parseHeader(expectedHeader);
        Assert.assertEquals(header.getNetIDEProtocolVersion(), output.getNetIDEProtocolVersion());
        Assert.assertEquals(header.getMessageType(), output.getMessageType());
        Assert.assertEquals(header.getPayloadLength(), output.getPayloadLength());
        Assert.assertEquals(header.getTransactionId(), output.getTransactionId());
        Assert.assertEquals(header.getModuleId(), output.getModuleId());
        Assert.assertEquals(header.getDatapathId(), output.getDatapathId());
    }

}
