/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import java.net.InetAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.openflowjava.protocol.api.connection.ThreadConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.TlsConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.TransportProtocol;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ShimConnectionConfigurationTest {
    @Mock
    InetAddress address;

    @Mock
    ThreadConfiguration threads;

    @Mock
    TlsConfiguration tlsConfiguration;

    TransportProtocol transportProtocol;

    ShimConnectionConfiguration shimConfiguration;

    @Before
    public void setUp() {
        transportProtocol = TransportProtocol.TCP;
        MockitoAnnotations.initMocks(this);
        shimConfiguration = new ShimConnectionConfiguration(address, 1, 1L, threads, tlsConfiguration,
                transportProtocol);
    }

    @Test
    public void test() {
        Assert.assertEquals(address, shimConfiguration.getAddress());
        Assert.assertEquals(1, shimConfiguration.getPort());
        Assert.assertNull(shimConfiguration.getSslContext());
        Assert.assertEquals(1L, shimConfiguration.getSwitchIdleTimeout());
        Assert.assertEquals(threads, shimConfiguration.getThreadConfiguration());
        Assert.assertEquals(tlsConfiguration, shimConfiguration.getTlsConfiguration());
        Assert.assertEquals(transportProtocol, shimConfiguration.getTransferProtocol());
    }
}
