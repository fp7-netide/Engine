package org.opendaylight.openflowjava.protocol.impl.serialization.factories;

import io.netty.buffer.ByteBuf;
import org.opendaylight.openflowjava.protocol.api.extensibility.OFSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketIn;

/**
 * Translates PacketIn messages
 */
public class PacketInMessageFactory implements OFSerializer<PacketIn> {

    @Override
    public void serialize(PacketIn message, ByteBuf outBuffer) {
        throw new UnsupportedOperationException();
    }

}