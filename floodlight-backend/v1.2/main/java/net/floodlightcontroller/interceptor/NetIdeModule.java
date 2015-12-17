/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import java.util.Collection;
import java.util.Map;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class NetIdeModule implements IFloodlightModule, IOFSwitchListener, IOFMessageListener {

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.IListener#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.floodlightcontroller.core.IListener#isCallbackOrderingPrereq(java.
     * lang.Object, java.lang.String)
     */
    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.floodlightcontroller.core.IListener#isCallbackOrderingPostreq(java.
     * lang.Object, java.lang.String)
     */
    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.IOFMessageListener#receive(net.
     * floodlightcontroller.core.IOFSwitch,
     * org.projectfloodlight.openflow.protocol.OFMessage,
     * net.floodlightcontroller.core.FloodlightContext)
     */
    @Override
    public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
            FloodlightContext cntx) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchAdded(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchAdded(DatapathId switchId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchRemoved(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchRemoved(DatapathId switchId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchActivated(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchActivated(DatapathId switchId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.floodlightcontroller.core.IOFSwitchListener#switchPortChanged(org.
     * projectfloodlight.openflow.types.DatapathId,
     * org.projectfloodlight.openflow.protocol.OFPortDesc,
     * net.floodlightcontroller.core.PortChangeType)
     */
    @Override
    public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchChanged(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchChanged(DatapathId switchId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.floodlightcontroller.core.module.IFloodlightModule#getModuleServices(
     * )
     */
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.floodlightcontroller.core.module.IFloodlightModule#getServiceImpls()
     */
    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.module.IFloodlightModule#
     * getModuleDependencies()
     */
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.module.IFloodlightModule#init(net.
     * floodlightcontroller.core.module.FloodlightModuleContext)
     */
    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.floodlightcontroller.core.module.IFloodlightModule#startUp(net.
     * floodlightcontroller.core.module.FloodlightModuleContext)
     */
    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        // TODO Auto-generated method stub

    }

}