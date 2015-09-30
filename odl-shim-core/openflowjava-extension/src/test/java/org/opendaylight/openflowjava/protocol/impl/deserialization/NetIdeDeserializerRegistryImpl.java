package org.opendaylight.openflowjava.protocol.impl.deserialization;

public class NetIdeDeserializerRegistryImpl extends DeserializerRegistryImpl{
	@Override
    public void init() {
		super.init();
		NetIdeMessageDeserializerInitializer.registerMessageDeserializers(this);
		InstructionDeserializerInitializer.registerDeserializers(this);
	}
}
