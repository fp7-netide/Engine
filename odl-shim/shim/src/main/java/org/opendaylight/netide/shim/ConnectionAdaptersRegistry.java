/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ConnectionAdaptersRegistry {

    private static HashMap<ConnectionAdapter, GetFeaturesOutput> connectionAdapterMap;

    public synchronized void init() {
        connectionAdapterMap = new LinkedHashMap<ConnectionAdapter, GetFeaturesOutput>();
    }

    public synchronized void setConnectionAdapterMap(HashMap<ConnectionAdapter, GetFeaturesOutput> map) {
        connectionAdapterMap = map;
    }

    public synchronized void registerConnectionAdapter(ConnectionAdapter connectionAdapter,
            GetFeaturesOutput datapathID) throws NullPointerException {
        connectionAdapterMap.put(connectionAdapter, datapathID);
    }

    public synchronized GetFeaturesOutput getFeaturesOutput(ConnectionAdapter connectionAdapter)
            throws NullPointerException {
        if (connectionAdapterMap.containsKey(connectionAdapter)) {
            return connectionAdapterMap.get(connectionAdapter);
        }
        return null;
    }

    public synchronized BigInteger getDatapathID(ConnectionAdapter connectionAdapter) throws NullPointerException {
        if (connectionAdapterMap.containsKey(connectionAdapter)) {
            GetFeaturesOutput obj = connectionAdapterMap.get(connectionAdapter);
            if (obj != null) {
                return obj.getDatapathId();
            }
        }
        return null;
    }

    public synchronized ConnectionAdapter getConnectionAdapter(Long datapathId) throws NullPointerException {
        for (ConnectionAdapter conn : connectionAdapterMap.keySet()) {
            if (connectionAdapterMap.get(conn).getDatapathId().longValue() == datapathId) {
                return conn;
            }
        }
        return null;
    }

    public Set<ConnectionAdapter> getConnectionAdapters() throws NullPointerException {
        return connectionAdapterMap.keySet();
    }

    public synchronized boolean removeConnectionAdapter(ConnectionAdapter conn) throws NullPointerException {
        GetFeaturesOutput datapathID = connectionAdapterMap.remove(conn);
        if (datapathID != null) {
            return true;
        }
        return false;
    }
}
