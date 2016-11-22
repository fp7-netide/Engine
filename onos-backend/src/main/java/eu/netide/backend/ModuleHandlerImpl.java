/*
 *  Copyright (c) 2016, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 *  Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 *  Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 *  Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors: Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.backend;

import eu.netide.lib.netip.ModuleAnnouncementMessage;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author Antonio Marsico (antonio.marsico@create-net.org)
 *
 */
public class ModuleHandlerImpl implements IModuleHandler {

    private HashMap<String, Integer> moduleRegistry;
    private NetIDEProtocolVersion netideVersion;

    private ZeroMQBaseConnector coreConnector;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public ModuleHandlerImpl(ZeroMQBaseConnector connector, NetIDEProtocolVersion version) {
        this.moduleRegistry = new HashMap<>();
        this.coreConnector = connector;
        this.netideVersion = version;
    }

    @Override
    public int getModuleId(String moduleName) {
        if (moduleRegistry.containsKey(moduleName)) {
            return moduleRegistry.get(moduleName);
        }

        return -1;
    }

    @Override
    public String getModuleNameFromID(int moduleId) {

        for (String key : moduleRegistry.keySet()) {
            if(moduleRegistry.get(key) == moduleId) {
                return key;
            }
        }

        return null;
    }

    @Override
    public void obtainModuleId(int xId, String moduleName, int backendId) {
        ModuleAnnouncementMessage msg = new ModuleAnnouncementMessage();
        msg.getHeader().setDatapathId(-1);
        msg.getHeader().setModuleId(backendId);
        //msg.getHeader().setNetIDEProtocolVersion(netideVersion);
        msg.getHeader().setTransactionId(xId);
        msg.setModuleName(moduleName);
        sendToCore(msg);
        boolean ack = false;

        //TODO: Handle the loop
        while (!ack) {
            if (moduleRegistry.containsKey(moduleName)) {
                ack = true;
            }
        }

    }

    @Override
    public void obtainBackendModuleId(int xId, String moduleName) {
        ModuleAnnouncementMessage msg = new ModuleAnnouncementMessage();
        msg.getHeader().setDatapathId(-1);
        msg.getHeader().setModuleId(-1);
        //msg.getHeader().setNetIDEProtocolVersion(netideVersion);
        msg.getHeader().setTransactionId(xId);
        msg.setModuleName(moduleName);
        sendToCore(msg);
        boolean ack = false;

        //TODO: Handle better the loop
        while (!ack) {
            if (moduleRegistry.containsKey(moduleName)) {
                ack = true;
            }
        }

    }

    @Override
    public void onModuleAckMessage(String moduleName, int moduleId) {
        log.debug("Module name : " + moduleName + " ModuleId: " + moduleId);
        moduleRegistry.put(moduleName, moduleId);
    }

    public boolean sendToCore(ModuleAnnouncementMessage msg) {
        return coreConnector.SendData(msg.toByteRepresentation());
    }

}
