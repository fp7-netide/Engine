/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.netiplib;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.javatuples.Pair;
import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistry;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializationFactory;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializerRegistryImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Utility methods for handling NetIP messages.
 */
public abstract class NetIPUtils {
    /**
     * Gets a stub header from the given payload with the length correctly set
     * and protocol version set to 1.1.
     *
     * @param payload
     *            the payload
     * @return the message header
     */

    public static MessageHeader StubHeaderFromPayload(byte[] payload) {
        MessageHeader h = new MessageHeader();
        h.setPayloadLength((short) payload.length);
        h.setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_2);
        return h;
    }

    /**
     * Concretizes the given message to the corresponding convenience class.
     *
     * @param message
     *            the message
     * @return the concretized message (e.g. an instance of OpenFlowMessage)
     */

    public static Message ConcretizeMessage(Message message) {
        try {
            switch (message.getHeader().getMessageType()) {
            case HELLO:
                return toHelloMessage(message);
            case ERROR:
                return toErrorMessage(message);
            case OPENFLOW:
                return toOpenFlowMessage(message);
            case NETCONF:
                return toNetconfMessage(message);
            case OPFLEX:
                return toOpFlexMessage(message);
            case MANAGEMENT:
                return toManagementMessage(message);
            case MODULE_ANNOUNCEMENT:
                return toModuleAnnouncementMessage(message);
            case MODULE_ACKNOWLEDGE:
                return toModuleAcknowledgeMessage(message);
            case TOPOLOGY_UPDATE:
                return toTopologyUpdateMessage(message);
            default:
                throw new IllegalArgumentException("Unknown message type.");
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not decode message. ", ex);
        }
    }

    /**
     * To hello message.
     *
     * @param message
     *            the message
     * @return the hello message
     */
    private static HelloMessage toHelloMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.HELLO)
            throw new IllegalArgumentException("Can only convert HELLO messages");
        HelloMessage hm = new HelloMessage();
        hm.setHeader(message.header);
        for (int i = 0; i < message.getPayload().length; i += 2) {
            Protocol protocol = Protocol.parse(message.getPayload()[i]);
            hm.getSupportedProtocols()
                    .add(new Pair<>(protocol, ProtocolVersions.parse(protocol, message.getPayload()[i + 1])));
        }
        return hm;
    }

    /**
     * To error message.
     *
     * @param message
     *            the message
     * @return the error message
     */
    private static ErrorMessage toErrorMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.ERROR)
            throw new IllegalArgumentException("Can only convert ERROR messages");
        ErrorMessage em = new ErrorMessage();
        em.setHeader(message.header);
        for (int i = 0; i < message.getPayload().length; i += 2) {
            Protocol protocol = Protocol.parse(message.getPayload()[i]);
            em.getSupportedProtocols()
                    .add(new Pair<>(protocol, ProtocolVersions.parse(protocol, message.getPayload()[i + 1])));
        }
        return em;
    }

    /**
     * To openflow message.
     *
     * @param message
     *            the message
     * @return the openflow message
     */
    private static OpenFlowMessage toOpenFlowMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.OPENFLOW)
            throw new IllegalArgumentException("Can only convert OPENFLOW messages");
        ByteBuf buffer = Unpooled.wrappedBuffer(message.getPayload());
        short ofVersion = buffer.readUnsignedByte();
        OpenFlowMessage ofm = new OpenFlowMessage(ofVersion);
        ofm.setPayload(message.getPayload());
        ofm.setHeader(message.header);

        // DESERIALIZATION
        DeserializerRegistry registry = new DeserializerRegistryImpl();
        registry.init();
        DeserializationFactory factory = new DeserializationFactory();
        factory.setRegistry(registry);

        DataObject dObj = factory.deserialize(buffer, ofVersion);
        ofm.setOfMessage(dObj);
        return ofm;
    }

    /**
     * To netconf message.
     *
     * @param message
     *            the message
     * @return the netconf message
     */
    private static NetconfMessage toNetconfMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.NETCONF)
            throw new IllegalArgumentException("Can only convert NETCONF messages");
        NetconfMessage ncm = new NetconfMessage();
        ncm.setHeader(message.header);
        ncm.setPayload(message.payload);
        return ncm;
    }

    /**
     * To op flex message.
     *
     * @param message
     *            the message
     * @return the op flex message
     */
    private static OpFlexMessage toOpFlexMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.OPFLEX)
            throw new IllegalArgumentException("Can only convert OPFLEX messages");
        OpFlexMessage ofm = new OpFlexMessage();
        ofm.setHeader(message.header);
        ofm.setPayload(message.payload);
        return ofm;
    }

    /**
     * To ManagementMessage.
     *
     * @param message
     *            the message
     * @return the management message
     */
    private static ManagementMessage toManagementMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.MANAGEMENT)
            throw new IllegalArgumentException("Can only convert MANAGEMENT messages");
        ManagementMessage ofm = new ManagementMessage();
        ofm.setHeader(message.header);
        ofm.setPayloadString(new String(message.getPayload()));
        return ofm;
    }

    /**
     * To ModuleAnnouncementMessage.
     *
     * @param message
     *            the message
     * @return the ModuleAnnouncement message
     */
    private static ModuleAnnouncementMessage toModuleAnnouncementMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.MODULE_ANNOUNCEMENT)
            throw new IllegalArgumentException("Can only convert MODULE_ANNOUNCEMENT messages");
        ModuleAnnouncementMessage mam = new ModuleAnnouncementMessage();
        mam.setHeader(message.header);
        mam.setModuleName(new String(message.getPayload()));
        return mam;
    }

    /**
     * To ModuleAcknowledgeMessage.
     *
     * @param message
     *            the message
     * @return the ModuleAcknowledge message
     */
    private static ModuleAcknowledgeMessage toModuleAcknowledgeMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.MODULE_ACKNOWLEDGE)
            throw new IllegalArgumentException("Can only convert MODULE_ACKNOWLEDGE messages");
        ModuleAcknowledgeMessage mam = new ModuleAcknowledgeMessage();
        mam.setHeader(message.header);
        mam.setModuleName(new String(message.getPayload()));
        return mam;
    }

    /**
     * To TopologyUpdateMessage.
     *
     * @param message
     *            the message
     * @return the TopologyUpdate message
     */
    private static TopologyUpdateMessage toTopologyUpdateMessage(Message message) {
        if (message.getHeader().getMessageType() != MessageType.TOPOLOGY_UPDATE)
            throw new IllegalArgumentException("Can only convert TOPOLOGY_UPDATE messages");
        TopologyUpdateMessage tum = new TopologyUpdateMessage();
        tum.setHeader(message.header);
        tum.setTopology(new String(message.getPayload()));
        return tum;
    }
}
