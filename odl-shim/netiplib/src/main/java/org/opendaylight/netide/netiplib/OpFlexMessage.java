/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

/**
 * Class representing a message of type OPFLEX.
 * Note that this only serves as a convenience class - if the MessageType is manipulated, the class will not recognize that.
 */
public class OpFlexMessage extends Message {

    /**
     * Instantiates a new Op flex message.
     */
    public OpFlexMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.OPFLEX);
    }

    /**
     * Sets payload.
     *
     * @param payload the payload
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public byte[] getPayload() {
        return this.payload;
    }
}
