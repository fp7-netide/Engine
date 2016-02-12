/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.shim;

import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.OpenFlowMessage;

/**
 * @author giuseppex.petralia@intel.com
 * @author kevin.phemius@thalesgroup.com
 */
public interface ICoreListener {

    void onHelloCoreMessage(HelloMessage msg);

	void onOpenFlowCoreMessage(OpenFlowMessage msg);

}
