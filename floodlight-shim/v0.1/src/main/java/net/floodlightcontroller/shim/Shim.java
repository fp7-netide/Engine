/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.shim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.javatuples.Pair;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netide.lib.netip.ErrorMessage;
import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.ModuleAcknowledgeMessage;
import eu.netide.lib.netip.ModuleAnnouncementMessage;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import eu.netide.lib.netip.OpenFlowMessage;
import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class Shim implements IFloodlightModule, IOFMessageListener, ICoreListener {

	protected IFloodlightProviderService floodlightProvider;
	protected IOFSwitchService switchProvider;
	protected static Logger logger;
	protected ZeroMQBaseConnector coreConnector;
	private OFVersion aggreedVersion;
	private List<Pair<Protocol, ProtocolVersions>> supportedProtocols = new ArrayList<Pair<Protocol,ProtocolVersions>>();

	private static final int NETIDE_CORE_PORT = 5555;
	private static final String NETIDE_CORE_ADDRESS = "127.0.0.1";
	private static final NetIDEProtocolVersion NETIDE_VERSION = NetIDEProtocolVersion.VERSION_1_2;

	private short XID_TIMEOUT = 5;
	private int XID_CRC_INDEX = 0;
	private Map<Long, XID_DB_Entry> XID_DB = new HashMap<Long,XID_DB_Entry>();
	private List<OFType> syncMessage = new ArrayList<OFType>();

	private class XID_DB_Entry{
		private long oldId;
		private int backendId;
		private long time;

		public XID_DB_Entry(long xid, int backenId, long time){
			this.oldId = xid;
			this.backendId = backenId;
			this.time = time;
		}
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IListener#getName()
	 */
	@Override
	public String getName() {
		String name="shim";
		return name;
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
		return false;
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

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#init(net.
	 * floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_0));
		supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_3));
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchProvider = context.getServiceImpl(IOFSwitchService.class);
		logger = LoggerFactory.getLogger(Shim.class);
		coreConnector = new ZeroMQBaseConnector(NETIDE_CORE_ADDRESS,NETIDE_CORE_PORT,this);
		// Define what is a synchronous message
		syncMessage.add(OFType.BARRIER_REPLY);
		syncMessage.add(OFType.ECHO_REPLY);
		syncMessage.add(OFType.FEATURES_REPLY);
		syncMessage.add(OFType.FLOW_REMOVED);
		syncMessage.add(OFType.GET_ASYNC_REPLY);
		syncMessage.add(OFType.GET_CONFIG_REPLY);
		syncMessage.add(OFType.QUEUE_GET_CONFIG_REPLY);
		syncMessage.add(OFType.ROLE_REPLY);
		syncMessage.add(OFType.STATS_REPLY);
		// Define what message the shim listens to
		List<OFType> ofmType = new ArrayList<>();
		ofmType.add(OFType.BARRIER_REPLY);
		ofmType.add(OFType.ECHO_REPLY);
		ofmType.add(OFType.FEATURES_REPLY);
		ofmType.add(OFType.ERROR);
		ofmType.add(OFType.FLOW_REMOVED);
		ofmType.add(OFType.GET_ASYNC_REPLY);
		ofmType.add(OFType.GET_CONFIG_REPLY);
		ofmType.add(OFType.PACKET_IN);
		ofmType.add(OFType.HELLO);
		ofmType.add(OFType.PORT_STATUS);
		ofmType.add(OFType.QUEUE_GET_CONFIG_REPLY);
		ofmType.add(OFType.ROLE_REPLY);
		ofmType.add(OFType.ROLE_STATUS);
		ofmType.add(OFType.STATS_REPLY);
		ofmType.add(OFType.TABLE_STATUS);
		for(OFType t : ofmType){
			floodlightProvider.addOFMessageListener(t,this);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#startUp(net.
	 * floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		coreConnector.Start();
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
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		logger.debug("Received message from switch "+sw.getId().getLong()+" type "+msg.getType());
		int moduleId = 0;
		OFMessage newMsg = null;
		if(syncMessage.contains(msg.getType())){
			XID_DB_Entry e = getXid(msg.getXid());
			moduleId = e.backendId;
			newMsg = setXid(msg, e.oldId);
		}else{
			newMsg=msg;
		}
		if(msg.getType()!=OFType.HELLO && newMsg != null)
			sendToClients(sw,newMsg,moduleId);
		return Command.CONTINUE; // ?
	}

	private void sendToClients(IOFSwitch sw, OFMessage ofmsg, int moduleId) {
		OpenFlowMessage msg = new OpenFlowMessage();
		msg.getHeader().setDatapathId(sw.getId().getLong());
		msg.getHeader().setTransactionId((int) ofmsg.getXid());
		msg.getHeader().setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_2);
		msg.getHeader().setModuleId(moduleId);
		msg.setOfMessage(ofmsg);
		logger.debug("Sending OF Message to Core " + msg.getOfMessage().getType().toString());
		coreConnector.sendData(msg.toByteRepresentation());
	}

	private void sendFeatureRequest(int backendId){
		for(IOFSwitch sw : switchProvider.getAllSwitchMap().values()){
			org.projectfloodlight.openflow.protocol.OFFeaturesRequest.Builder req = sw.getOFFactory().featuresRequest().createBuilder();
			req.setXid(storeXid(backendId, backendId));
			sw.write(req.build());
		}
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.interceptor.CoreListener#onOpenFlowCoreMessage(
	 * java.lang.Long, io.netty.buffer.ByteBuf, int)
	 */
	@Override
	public void onOpenFlowCoreMessage(OpenFlowMessage msg) {
		purgeXid();
		if(msg.getHeader().getPayloadLength()==0)
			return;
		IOFSwitch sw = switchProvider.getSwitch(DatapathId.of(msg.getHeader().getDatapathId()));
		if(sw!=null){
			int backendId = msg.getHeader().getModuleId();
			if(backendId!=0){
				long newXid = storeXid(msg.getHeader().getTransactionId(),backendId);
				OFMessage newMsg = setXid(msg.getOfMessage(),newXid);
				sw.write(newMsg);
			}
		}
	}

	private OFMessage setXid(OFMessage msg, long newXid) {
		org.projectfloodlight.openflow.protocol.OFMessage.Builder newMsg = msg.createBuilder();
		newMsg.setXid(newXid);
		return newMsg.build();
	}

	private XID_DB_Entry getXid(long l){
		if(XID_DB.containsKey(l)){
			return XID_DB.remove(l);
		}
		return null;
	}
	private long storeXid(long transactionId, int backendId) {
		long newXid;
		while(true){
			newXid = String.valueOf(transactionId).hashCode() + String.valueOf(backendId).hashCode() + XID_CRC_INDEX;
			if(XID_DB.get(newXid)!=null)
				break;
		}
		XID_DB.put(newXid, new XID_DB_Entry(transactionId, backendId, System.currentTimeMillis()));
		return newXid;
	}

	private void purgeXid() {
		long time = System.currentTimeMillis();
		for(Entry<Long, XID_DB_Entry> e : XID_DB.entrySet()){
			if(time > e.getValue().time + XID_TIMEOUT*1000)
				XID_DB.remove(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.interceptor.CoreListener#onHelloCoreMessage(java
	 * .util.List, int)
	 */
	@Override
	public void onHelloCoreMessage(HelloMessage msg) {
		if(msg.getHeader().getNetIDEProtocolVersion()!=NETIDE_VERSION){
			logger.info("Attempt to connect from unsupported client");
			return;
		}
		boolean noSupport = true;
		for(Pair<Protocol, ProtocolVersions> protocol : msg.getSupportedProtocols()){
			if(supportedProtocols.contains(protocol)){
				noSupport = false;
				break;
			}
		}
		if(noSupport){
			logger.info("WARNING: Client does not support any protocol");
			return;
		}
		// if(the_controoler_is_not_up)
		//error("there are no switch it's weird")
		int backendId = msg.getHeader().getModuleId();
		logger.info("Got HELLO message from backend with module ID "+backendId+".\nSupported protocols:"+msg.getSupportedProtocols());
		byte[] data = msg.getPayload();
		List<Pair<Protocol, ProtocolVersions>> negotiatedProtocols = new ArrayList<>();
		short count = 0;
		while(count<data.length){
			byte protocol = data[count];
			byte version = data[count+1];
			count+=2;
			if (protocol==Protocol.OPENFLOW.getValue()
					&& version==ProtocolVersions.OPENFLOW_1_3.getValue()) //Only 1.3 for now
				negotiatedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_3));
		}
		if(negotiatedProtocols.isEmpty()){
			logger.info("Unsupported protocol or protocol mismatch");
			ErrorMessage ret = new ErrorMessage();
			ret.getHeader().setModuleId(backendId);
			coreConnector.sendData(ret.toByteRepresentation());
		}else{
			HelloMessage hello = new HelloMessage();
			hello.setSupportedProtocols(negotiatedProtocols);
			hello.getHeader().setNetIDEProtocolVersion(NETIDE_VERSION);
			hello.getHeader().setPayloadLength((short)hello.getPayload().length);
			hello.getHeader().setModuleId(backendId);
			coreConnector.sendData(hello.toByteRepresentation());
			logger.debug("Sent hello ack to backend "+backendId+" with protocol(s)"+supportedProtocols);
			sendFeatureRequest(backendId);
		}
	}
	private int modId = 42;
	@Override
	public void onModuleAnnouncementMessage(ModuleAnnouncementMessage msg){
		logger.info("Received Module Announcement message from "+msg.getModuleName());
		ModuleAcknowledgeMessage ack = new ModuleAcknowledgeMessage();
		ack.getHeader().setPayloadLength((short) msg.getPayload().length);
		ack.setModuleName(msg.getModuleName());
		ack.getHeader().setModuleId(modId);
		logger.info("Sent ack to module "+msg.getModuleName()+" with ID "+modId+".");
		coreConnector.sendData(ack.toByteRepresentation());
		modId++;
	}

}
