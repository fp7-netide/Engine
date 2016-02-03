/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.javatuples.Pair;
import org.opendaylight.netide.netiplib.Protocol;
import org.opendaylight.netide.netiplib.ProtocolVersions;

public interface ICoreListener {

    void onOpenFlowCoreMessage(Long datapathId, ByteBuf msg, int moduleId);

    void onHelloCoreMessage(List<Pair<Protocol, ProtocolVersions>> requiredVersion, int moduleId);
}