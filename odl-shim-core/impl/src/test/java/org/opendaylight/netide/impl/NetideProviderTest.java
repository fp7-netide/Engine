/*
 * Copyright NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.impl;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

import static org.mockito.Mockito.mock;

public class NetideProviderTest {
    @Test
    public void testOnSessionInitiated() {
        NetideProvider provider = new NetideProvider();

        // ensure no exceptions
        // currently this method is empty
        provider.onSessionInitiated(mock(BindingAwareBroker.ProviderContext.class));
    }

    @Test
    public void testClose() throws Exception {
        NetideProvider provider = new NetideProvider();

        // ensure no exceptions
        // currently this method is empty
        provider.close();
    }
    @Test
    public void testNexus() {
    	assert(true);
    }
}
