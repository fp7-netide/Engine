package org.opendaylight.netide.shim;

import eu.netide.lib.netip.Message;

public interface ICoreListener {
	
	void onCoreMessage(Message input);
}
