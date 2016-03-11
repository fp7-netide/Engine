/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.impl;

import java.util.concurrent.Future;
import org.opendaylight.netide.netiplib.NetIDEProtocolVersion;
import org.opendaylight.netide.shim.ShimSwitchConnectionHandlerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netide.rev151001.NetideService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netide.rev151001.StatusOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netide.rev151001.StatusOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class StatusImpl implements NetideService {

    private ShimSwitchConnectionHandlerImpl connectionHandler;

    public StatusImpl(ShimSwitchConnectionHandlerImpl handler) {
        connectionHandler = handler;
    }

    @Override
    public Future<RpcResult<StatusOutput>> status() {
        StatusOutputBuilder builder = new StatusOutputBuilder();
        builder.setNetipVersion(NetIDEProtocolVersion.VERSION_1_1.getValue());
        builder.setOfVersions(connectionHandler.getSupportedOFProtocols());
        builder.setConnectedSwitches(connectionHandler.getNumberOfSwitches());
        return RpcResultBuilder.success(builder.build()).buildFuture();
    }
}