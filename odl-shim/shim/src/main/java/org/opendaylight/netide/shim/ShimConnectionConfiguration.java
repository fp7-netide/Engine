/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import java.net.InetAddress;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.ThreadConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.TlsConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.TransportProtocol;

public class ShimConnectionConfiguration implements ConnectionConfiguration {
    private InetAddress _inetAddress;
    private int _port;
    private long _switchIdleTimeout;
    private ThreadConfiguration _threads;
    private TlsConfiguration _tlsConfiguration;
    private TransportProtocol _transportProtocol;

    public ShimConnectionConfiguration(InetAddress address, int port, long switchIdleTimeout,
            ThreadConfiguration threads, TlsConfiguration tlsConfiguration, TransportProtocol transportProtocol) {
        _inetAddress = address;
        _port = port;
        _switchIdleTimeout = switchIdleTimeout;
        _threads = threads;
        _tlsConfiguration = tlsConfiguration;
        _transportProtocol = transportProtocol;
    }

    @Override
    public InetAddress getAddress() {
        return _inetAddress;
    }

    @Override
    public int getPort() {
        return _port;
    }

    @Override
    public Object getSslContext() {
        return null;
    }

    @Override
    public long getSwitchIdleTimeout() {
        return _switchIdleTimeout;
    }

    @Override
    public ThreadConfiguration getThreadConfiguration() {
        return _threads;
    }

    @Override
    public TlsConfiguration getTlsConfiguration() {
        return _tlsConfiguration;
    }

    @Override
    public Object getTransferProtocol() {
        return _transportProtocol;
    }

    @Override
    public boolean useBarrier() {
        return false;
    }

}
