/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import java.util.concurrent.Future;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor;
import org.opendaylight.openflowplugin.api.openflow.md.core.ErrorHandler;
import org.opendaylight.openflowplugin.api.openflow.md.core.SwitchConnectionDistinguisher;
import org.opendaylight.openflowplugin.api.openflow.md.core.session.SessionContext;
import org.opendaylight.openflowplugin.api.openflow.md.queue.QueueProcessor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.OfHeader;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ShimConductor implements ConnectionConductor {
    short version;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * init()
     */
    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * getVersion()
     */
    @Override
    public Short getVersion() {
        // TODO Auto-generated method stub
        return version;
    }

    public void setVersion(short _version) {
        version = _version;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * getConductorState()
     */
    @Override
    public CONDUCTOR_STATE getConductorState() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * setConductorState(org.opendaylight.openflowplugin.api.openflow.md.core.
     * ConnectionConductor.CONDUCTOR_STATE)
     */
    @Override
    public void setConductorState(CONDUCTOR_STATE conductorState) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * disconnect()
     */
    @Override
    public Future<Boolean> disconnect() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * setSessionContext(org.opendaylight.openflowplugin.api.openflow.md.core.
     * session.SessionContext)
     */
    @Override
    public void setSessionContext(SessionContext context) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * setConnectionCookie(org.opendaylight.openflowplugin.api.openflow.md.core.
     * SwitchConnectionDistinguisher)
     */
    @Override
    public void setConnectionCookie(SwitchConnectionDistinguisher auxiliaryKey) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * getSessionContext()
     */
    @Override
    public SessionContext getSessionContext() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * getAuxiliaryKey()
     */
    @Override
    public SwitchConnectionDistinguisher getAuxiliaryKey() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * getConnectionAdapter()
     */
    @Override
    public ConnectionAdapter getConnectionAdapter() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * setQueueProcessor(org.opendaylight.openflowplugin.api.openflow.md.queue.
     * QueueProcessor)
     */
    @Override
    public void setQueueProcessor(QueueProcessor<OfHeader, DataObject> queueKeeper) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * setErrorHandler(org.opendaylight.openflowplugin.api.openflow.md.core.
     * ErrorHandler)
     */
    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.openflowplugin.api.openflow.md.core.ConnectionConductor#
     * setId(int)
     */
    @Override
    public void setId(int conductorId) {
        // TODO Auto-generated method stub

    }

}
