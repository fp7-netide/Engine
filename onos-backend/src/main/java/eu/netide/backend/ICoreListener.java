/*
 *  Copyright (c) 2016, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 *  Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 *  Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 *  Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors: Antonio Marsico (antonio.marsico@create-net.org)
 */

package eu.netide.backend;

/**
 * Created by antonio on 05/02/16.
 */

import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import io.netty.buffer.ByteBuf;
import org.javatuples.Pair;

import java.util.List;

public interface ICoreListener {

    void onOpenFlowCoreMessage(Long datapathId, ByteBuf msg, int moduleId, int transactionId);

    void onHelloCoreMessage(List<Pair<Protocol, ProtocolVersions>> requestedProtocols, int moduleId);
}