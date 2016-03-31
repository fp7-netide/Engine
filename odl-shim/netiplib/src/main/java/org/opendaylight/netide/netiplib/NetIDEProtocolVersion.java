/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

/**
 * Created by timvi on 06.08.2015.
 */
public enum NetIDEProtocolVersion {
    /**
     * The Version 1.0
     */
    VERSION_1_0((byte) 0x01),
    /**
     * The Version 1.1
     */
    VERSION_1_1((byte) 0x02),
    /**
     * The Version 1.2
     */
    VERSION_1_2((byte) 0x03);

    private byte value;

    /**
     * Instantiates a new Net iDE protocol version.
     *
     * @param value the value
     */
    NetIDEProtocolVersion(byte value) {
        this.value = value;
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
     * Parse net iDE protocol version.
     *
     * @param value the value
     * @return the net iDE protocol version
     */
    public static NetIDEProtocolVersion parse(final byte value) {
        for (NetIDEProtocolVersion c : NetIDEProtocolVersion.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unexpected value " + value);
    }
}
