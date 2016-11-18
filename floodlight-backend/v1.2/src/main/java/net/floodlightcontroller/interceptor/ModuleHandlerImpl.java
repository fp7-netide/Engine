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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ModuleHandlerImpl implements IModuleHandler {

	public HashMap<String, Integer> moduleRegistry;

	private ZeroMQBaseConnector coreConnector;
	private static final Logger LOG = LoggerFactory.getLogger(ModuleHandlerImpl.class);

	public ModuleHandlerImpl(ZeroMQBaseConnector connector) {
		moduleRegistry = new HashMap<>();
		coreConnector = connector;
	}

	@Override
	public int getModuleId(int xId, String moduleName) {
		if (moduleRegistry.containsKey(moduleName)) {
			return moduleRegistry.get(moduleName);
		}

		return -1;
	}

	@Override
	public String getModuleName(int moduleID) {
		for (String moduleName : moduleRegistry.keySet()) {
			if (moduleRegistry.get(moduleName) == moduleID)
				return moduleName;
		}
		return null;
	}

	@Override
	public void obtainModuleId(int xId, String moduleName) {
		LOG.debug("ObtainModuleID for " + moduleName);
		ModuleAnnouncementMessage msg = new ModuleAnnouncementMessage();
		msg.getHeader().setDatapathId(-1);
		msg.getHeader().setModuleId(-1);
		msg.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_4);
		msg.getHeader().setTransactionId(xId);
		msg.setModuleName(moduleName);
		sendToCore(msg);
		boolean ack = false;

		while (!ack) {
			if (moduleRegistry.containsKey(moduleName)) {
				ack = true;
			}
		}

	}

	@Override
	public void onModuleAckMessage(String moduleName, int moduleId) {
		LOG.debug("Module name : " + moduleName + " ModuleId: " + moduleId);
		moduleRegistry.put(moduleName, moduleId);
	}

	public void sendToCore(ModuleAnnouncementMessage msg) {
		coreConnector.SendData(msg.toByteRepresentation());
	}

}
