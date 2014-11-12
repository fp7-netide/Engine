/** 
 Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html
 
 Authors:
     aleckey
 */
package net.floodlightcontroller.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.openflow.protocol.OFError;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;

/**
 * Creates a new backend channel for floodlight that connects to an ODL instance
 *
 *
 */
public class BackendChannel implements IFloodlightModule, IOFSwitchListener, IOFMessageListener {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	
	/////////////////////////// IFloodlightModule methods /////////////////////////
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(BackendChannel.class);
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) {
		//ADD SWITCH LISTENERS
		floodlightProvider.addOFSwitchListener(this);
		
		//REGISTER FOR MESSAGES FROM THE SWITCHES
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		floodlightProvider.addOFMessageListener(OFType.PACKET_OUT, this);
		floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.ERROR, this);
	}

	/////////////////////////// IOFSwitchListener methods /////////////////////////
	@Override
	public void addedSwitch(IOFSwitch sw) {
		
	}

	@Override
	public void removedSwitch(IOFSwitch sw) {
		
	}

	@Override
	public void switchPortChanged(Long switchId) {
		
	}

	/////////////////////////// IOFMessageListener methods ////////////////////
	@Override
	public String getName() {
		return BackendChannel.class.getSimpleName();
	}
	
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
			return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}
	
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		logger.info(msg.toString());
		
		switch (msg.getType()) {
	        case PACKET_IN:
	        	
	        case FLOW_REMOVED:
	        	
	        case ERROR:
	            logger.info("received an error {} from switch {}", (OFError) msg, sw);
	            return Command.CONTINUE;
	        default:
	        	break;
		}
		return Command.CONTINUE;
	}
		
}
