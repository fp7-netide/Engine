/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * @author giuseppex.petralia@intel.com
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class ZeroMQBaseConnectorTest {

    private static final String CONTROL_ADDRESS = "inproc://ShimControllerQueue";

    @Mock
    ZMsg msg;

    @Mock
    ZMQ.Socket sendSocket;

    @Mock
    ZMQ.Context context;

    ZeroMQBaseConnector connector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(ZeroMQBaseConnectorTest.class);
        connector = Mockito.spy(new ZeroMQBaseConnector());
        connector.Start();

    }

    @Test(timeout = 5000)
    public void testSendData() throws InterruptedException {
        byte[] data = new byte[] { 1, 2, 3 };
        Mockito.stub(connector.send(Matchers.any(ZMsg.class), Matchers.any(ZMQ.Socket.class))).toReturn(true);
        Assert.assertTrue(connector.SendData(data));
    }

}
