/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

import java.util.ArrayList;
import java.util.List;
import org.javatuples.Pair;

/**
 * Class representing a message of type HELLO.
 * Note that this only serves as a convenience class - if the MessageType is manipulated, the class will not recognize that.
 */
public class HelloMessage extends Message {
    /**
     * The list of supported protocols and their versions.
     */
    private List<Pair<Protocol, ProtocolVersions>> supportedProtocols;

    /**
     * Creates a new instance of the HelloMessage class.
     */
    public HelloMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.HELLO);
        supportedProtocols = new ArrayList<>();
    }

    /**
     * Returns the list of mappings of supported protocols.
     *
     * @return The list of mappings of supported protocols.
     */
    public List<Pair<Protocol, ProtocolVersions>> getSupportedProtocols() {
        return supportedProtocols;
    }

    /**
     * Sets the list of mappings of supported protocols.
     *
     * @param supportedProtocols The new list of mappings of supported protocols.
     */
    public void setSupportedProtocols(List<Pair<Protocol, ProtocolVersions>> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }


    @Override
    public byte[] getPayload() {
        byte[] payload = new byte[supportedProtocols.size() * 2];
        int i = 0;
        for (Pair<Protocol, ProtocolVersions> entry : supportedProtocols) {
            payload[i] = entry.getValue0().getValue();
            payload[i + 1] = entry.getValue1().getValue();
            i += 2;
        }
        this.payload = payload;
        return this.payload;
    }
}
