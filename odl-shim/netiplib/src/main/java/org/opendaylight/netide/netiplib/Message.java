/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

/**
 * Class representing a simple NetIP message. This also is the base class for
 * concrete message classes.
 */
public class Message {
    /**
     * The header.
     */
    protected MessageHeader header;
    /**
     * The payload.
     */
    protected byte[] payload;

    /**
     * Creates a new instance of the Message class.
     *
     * @param header
     *            The header to use.
     * @param payload
     *            The payload.
     */
    public Message(MessageHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    /**
     * Gets the header.
     *
     * @return the header.
     */
    public MessageHeader getHeader() {
        return header;
    }

    /**
     * Sets the header.
     *
     * @param header
     *            The new header.
     */
    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    /**
     * Gets the current payload as bytes.
     *
     * @return the payload ImplNote: This method has to ensure that the returned
     *         payload reflects the current state of any convenience fields!
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Returns the message's byte representation, including the header.
     *
     * @return The byte representation.
     */
    public byte[] toByteRepresentation() {
        byte[] payload = getPayload();

        byte[] bytes = new byte[MessageHeader.HEADER_BYTES + payload.length];
        System.arraycopy(header.toByteRepresentation(), 0, bytes, 0, MessageHeader.HEADER_BYTES);
        System.arraycopy(payload, 0, bytes, MessageHeader.HEADER_BYTES, payload.length);
        return bytes;
    }

    public void setPayload(byte[] data) {
        this.payload = data;
    }
}
