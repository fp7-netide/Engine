package org.opendaylight.openflowjava.protocol.impl.deserialization.factories;

import io.netty.buffer.ByteBuf;

import org.opendaylight.openflowjava.protocol.api.extensibility.OFDeserializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInput;

/**
 * Translates FlowModInput messages
 */
public class FlowModInputMessageFactory implements OFDeserializer<FlowModInput> {

    @Override
    public FlowModInput deserialize(ByteBuf rawMessage) {
    	throw new UnsupportedOperationException();
    }
}