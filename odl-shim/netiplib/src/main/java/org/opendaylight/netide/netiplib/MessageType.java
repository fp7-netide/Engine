/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

/**
 * Enumeration of MessageTypes according to the NetIP specification.
 */
public enum MessageType {
    /**
     * The HELLO type.
     */
    HELLO((byte) 0x01),
    /**
     * The ERROR type.
     */
    ERROR((byte) 0x02),
    /**
     * The MANAGEMENT type.
     */
    MANAGEMENT((byte) 0x03),
    /**
     * The MODULE_ANNOUNCEMENT.
     */
    MODULE_ANNOUNCEMENT((byte) 0x04),
    /**
     * The MODULE_ACKNOWLEDGE.
     */
    MODULE_ACKNOWLEDGE((byte) 0x05),
    /**
     * The TOPOLOGY_UPDATE.
     */
    TOPOLOGY_UPDATE((byte) 0x06),
    /**
     * The OPENFLOW type.
     */
    OPENFLOW(Protocol.OPENFLOW.getValue()),
    /**
     * The NETCONF type.
     */
    NETCONF(Protocol.NETCONF.getValue()),
    /**
     * The OPFLEX type.
     */
    OPFLEX(Protocol.OPFLEX.getValue()),
    /**
     * The UNSUPPORTED type.
     */
    UNSUPPORTED((byte) 0x00);

    private byte value;

    /**
     * Instantiates a new Message type.
     *
     * @param value the value
     */
    MessageType(byte value) {
        this.value = value;
    }

    /**
     * Gets the byte value.
     *
     * @return the value
     */
    public byte getValue() {
        return this.value;
    }

    /**
     * Parse message type.
     *
     * @param value the value
     * @return the message type
     */
    public static MessageType parse(final byte value) {
        for (MessageType c : MessageType.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
