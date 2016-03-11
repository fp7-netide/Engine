/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

/**
 * Created by timvi on 24.09.2015.
 */
public class TopologyUpdateMessage extends Message {

    private String topology;

    /**
     * Instantiates a new Topology update message.
     */
    public TopologyUpdateMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.TOPOLOGY_UPDATE);
    }

    /**
     * Gets topology.
     *
     * @return the topology
     */
    public String getTopology() {
        return topology;
    }

    /**
     * Sets topology.
     *
     * @param topology the topology
     */
    public void setTopology(String topology) {
        this.topology = topology;
    }

    @Override
    public byte[] getPayload() {
        return topology.getBytes();
    }
}
