/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.netide.shim.ShimSwitchConnectionHandlerImpl;
import org.opendaylight.netide.shim.ZeroMQBaseConnector;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration;
import org.opendaylight.openflowjava.protocol.impl.core.SwitchConnectionProviderImpl;
import org.opendaylight.openflowjava.protocol.spi.connection.SwitchConnectionProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netide.rev151001.NetideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetideProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NetideProvider.class);
    private RpcRegistration<NetideService> netideService;
    private SwitchConnectionProvider connectionProvider;
    private ZeroMQBaseConnector coreConnector;
    private int corePort;
    private String coreAddress;
    ConnectionConfiguration conf;
    NotificationPublishService notificationProviderService;

    public NetideProvider(String _coreAddress, int _corePort, ConnectionConfiguration connectionConfiguration,
            NotificationPublishService _notificationProviderService) {
        coreAddress = _coreAddress;
        corePort = _corePort;
        conf = connectionConfiguration;
        notificationProviderService = _notificationProviderService;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NetideProvider Session Initiated");
        connectionProvider = new SwitchConnectionProviderImpl();
        coreConnector = new ZeroMQBaseConnector();

        ShimSwitchConnectionHandlerImpl handler = new ShimSwitchConnectionHandlerImpl(coreConnector,
                notificationProviderService);
        handler.init();

        coreConnector.RegisterCoreListener(handler);
        coreConnector.setAddress(coreAddress);
        coreConnector.setPort(corePort);

        connectionProvider.setSwitchConnectionHandler(handler);

        connectionProvider.setConfiguration(conf);
        coreConnector.Start();
        connectionProvider.startup();

        netideService = session.addRpcImplementation(NetideService.class, new StatusImpl(handler));
    }

    @Override
    public void close() throws Exception {
        LOG.info("NetideProvider Closed");
        if (netideService != null) {
            netideService.close();
        }
        connectionProvider.shutdown();
        coreConnector.Stop();
    }

}