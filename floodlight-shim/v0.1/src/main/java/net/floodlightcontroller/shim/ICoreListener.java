/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.shim;

import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.ModuleAnnouncementMessage;
import eu.netide.lib.netip.OpenFlowMessage;
import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import java.util.List;
import org.javatuples.Pair;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * @author giuseppex.petralia@intel.com
 * @author kevin.phemius@thalesgroup.com
 */
public interface ICoreListener {

    void onHelloCoreMessage(HelloMessage msg);

	void onOpenFlowCoreMessage(OpenFlowMessage msg);

	void onModuleAnnouncementMessage(ModuleAnnouncementMessage msg);

}
