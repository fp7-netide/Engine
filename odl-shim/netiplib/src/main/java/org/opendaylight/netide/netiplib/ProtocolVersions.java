/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

/**
 * Enumeration of known protocol versions. Useful for Protocol to supportedVersion mappings.
 */
public enum ProtocolVersions {
    /**
     * The OPENFLOW_1_0.
     */
    OPENFLOW_1_0(Protocol.OPENFLOW, (byte) 0x01),
    /**
     * The OPENFLOW_1_1.
     */
    OPENFLOW_1_1(Protocol.OPENFLOW, (byte) 0x02),
    /**
     * The OPENFLOW_1_2.
     */
    OPENFLOW_1_2(Protocol.OPENFLOW, (byte) 0x03),
    /**
     * The OPENFLOW_1_3.
     */
    OPENFLOW_1_3(Protocol.OPENFLOW, (byte) 0x04),
    /**
     * The OPENFLOW_1_4.
     */
    OPENFLOW_1_4(Protocol.OPENFLOW, (byte) 0x05),
    /**
     * The NETCONF_1_0.
     */
    NETCONF_1_0(Protocol.NETCONF, (byte) 0x01),
    /**
     * The OPFLEX_0_0.
     */
    OPFLEX_0_0(Protocol.OPFLEX, (byte) 0x00);

    private Protocol protocol;
    private byte value;

    /**
     * Instantiates a new Protocol versions.
     *
     * @param value the value
     */
    ProtocolVersions(Protocol protocol, byte value) {
        this.value = value;
        this.protocol = protocol;
    }

    /**
     * Gets the protocol.
     *
     * @return the protocol
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public byte getValue() {
        return this.value;
    }

    /**
     * Parse protocol versions.
     *
     * @param value the value
     * @return the protocol versions
     */
    public static ProtocolVersions parse(final Protocol protocol, final byte value) {
        for (ProtocolVersions c : ProtocolVersions.values()) {
            if (c.value == value && c.protocol == protocol) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
