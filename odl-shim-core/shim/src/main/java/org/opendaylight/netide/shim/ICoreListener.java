package org.opendaylight.netide.shim;

import io.netty.buffer.ByteBuf;

public interface ICoreListener {
	
	void onCoreMessage(ByteBuf input);
}
