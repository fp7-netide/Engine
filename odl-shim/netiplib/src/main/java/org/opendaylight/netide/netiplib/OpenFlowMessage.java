/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.opendaylight.openflowjava.protocol.api.extensibility.SerializerRegistry;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializationFactory;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializerRegistryImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Class representing a message of type OPENFLOW. Note that this only serves as
 * a convenience class - if the MessageType is manipulated, the class will not
 * recognize that.
 */
public class OpenFlowMessage extends Message {
    private DataObject ofMessage;
    private final short ofVersion;

    /**
     * Instantiates a new Open flow message.
     */
    public OpenFlowMessage(short version) {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.OPENFLOW);
        ofVersion = version;
    }

    public short getOfVersion() {
        return ofVersion;
    }

    /**
     * Gets of message.
     *
     * @return the OF message
     */
    public DataObject getOfMessage() {
        return ofMessage;
    }

    /**
     * Sets of message.
     *
     * @param ofMessage
     *            the OF message
     */
    public void setOfMessage(DataObject ofMessage) {
        this.ofMessage = ofMessage;
    }

    @Override
    public byte[] getPayload() {
        SerializerRegistry registry = new SerializerRegistryImpl();
        registry.init();
        SerializationFactory factory = new SerializationFactory();
        factory.setSerializerTable(registry);
        ByteBuf output = UnpooledByteBufAllocator.DEFAULT.buffer();
        factory.messageToBuffer(getOfVersion(), output, getOfMessage());
        byte[] rawPayload = new byte[output.readableBytes()];
        output.getBytes(0, rawPayload);
        return rawPayload;
    }
}
