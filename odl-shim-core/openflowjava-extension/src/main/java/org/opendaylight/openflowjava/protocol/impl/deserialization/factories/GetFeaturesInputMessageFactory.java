package org.opendaylight.openflowjava.protocol.impl.deserialization.factories;

import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistry;
import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistryInjector;
import org.opendaylight.openflowjava.protocol.api.extensibility.OFDeserializer;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesInputBuilder;

import io.netty.buffer.ByteBuf;

public class GetFeaturesInputMessageFactory implements OFDeserializer<GetFeaturesInput>,  DeserializerRegistryInjector {
	private DeserializerRegistry registry;
	
    @Override
    public GetFeaturesInput deserialize(ByteBuf rawMessage) {
    	GetFeaturesInputBuilder builder = new GetFeaturesInputBuilder();
    	builder.setVersion((short) EncodeConstants.OF13_VERSION_ID);
    	builder.setXid(rawMessage.readUnsignedInt());
    	return builder.build();
    }
    
    @Override
    public void injectDeserializerRegistry(DeserializerRegistry deserializerRegistry) {
        registry = deserializerRegistry;
    }
}