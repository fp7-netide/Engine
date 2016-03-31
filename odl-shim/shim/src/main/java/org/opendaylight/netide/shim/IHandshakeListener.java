/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public interface IHandshakeListener {

    public void onSwitchHelloMessage(long xid, Short version, ConnectionAdapter connectionAdapter);

    public void onSwitchDisconnected(ConnectionAdapter connectionAdapter);
}
