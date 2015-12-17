/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.javatuples.Pair;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public interface CoreListener {
    void onOpenFlowCoreMessage(Long datapathId, ByteBuf msg, int moduleId);

    void onHelloCoreMessage(List<Pair<Protocol, ProtocolVersions>> requiredVersion, int moduleId);

}
