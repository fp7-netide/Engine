/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import eu.netide.lib.netip.ModuleAnnouncementMessage;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import java.util.HashMap;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ModuleHandlerImpl implements IModuleHandler, IModuleListener {

    public HashMap<String, Integer> moduleRegistry;

    private ZeroMQBaseConnector coreConnector;

    public ModuleHandlerImpl(ZeroMQBaseConnector connector) {
        moduleRegistry = new HashMap<>();
        coreConnector = connector;
    }

    @Override
    public synchronized int getModuleId(int xId, String moduleName) {
        if (!moduleRegistry.containsKey(moduleName)) {
            moduleRegistry.put(moduleName, -1);
            obtainModuleId(xId, moduleName);
        }

        return moduleRegistry.get(moduleName);
    }

    private synchronized void obtainModuleId(int xId, String moduleName) {
        ModuleAnnouncementMessage msg = new ModuleAnnouncementMessage();
        msg.getHeader().setDatapathId(-1);
        msg.getHeader().setModuleId(-1);
        msg.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_2);
        msg.getHeader().setTransactionId(xId);
        sendToCore(msg);

        boolean ack = false;

        while (!ack) {
            if (moduleRegistry.get(moduleName) != -1) {
                ack = true;
            }
        }

    }

    @Override
    public synchronized void onModuleAckMessage(String moduleName, int moduleId) {
        moduleRegistry.put(moduleName, moduleId);
    }

    public void sendToCore(ModuleAnnouncementMessage msg) {
        coreConnector.SendData(msg.toByteRepresentation());
    }

}
