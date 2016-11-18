/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.floodlightcontroller.core.IOFConnection;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.LogicalOFMessageCategory;
import net.floodlightcontroller.core.OFConnection;
import net.floodlightcontroller.core.SwitchDescription;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class DummySwitch implements IOFSwitch {

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFMessageWriter#write(org.
	 * projectfloodlight.openflow.protocol.OFMessage)
	 */
	protected static Logger logger;
	private long datapathId;
	private OFFeaturesReply features;
	private boolean handshakeCompleted = false;
	private boolean statsReplyReceived = false;
	private boolean connectionHandshakeCompleted = false;
	public OFType lastMsgType = OFType.ERROR;
	private ZeroMQBaseConnector coreConnector;
	private String moduleName;
	private IModuleHandler moduleHandler;

	public DummySwitch(long _datapathId, OFFeaturesReply features, String moduleName, ZeroMQBaseConnector coreConnector,
			IModuleHandler moduleHandler) {
		datapathId = _datapathId;
		this.features = features;
		this.moduleName = moduleName;
		this.coreConnector = coreConnector;
		this.moduleHandler = moduleHandler;
		logger = LoggerFactory.getLogger(DummySwitch.class);
	}

	@Override
	public OFFactory getOFFactory() {
		return OFFactories.getFactory(features.getVersion());
	}

	public boolean isConnectionHandshakeCompleted() {
		return connectionHandshakeCompleted;
	}

	public void setConnectionHandshake(boolean value) {
		connectionHandshakeCompleted = value;
	}

	public boolean isStatsReplyReceived() {
		return statsReplyReceived;
	}

	public void setStatsReplyReceived(boolean value) {
		statsReplyReceived = value;
	}

	public boolean isHandshakeCompleted() {
		return handshakeCompleted;
	}

	public void setHandshakeCompleted(boolean value) {
		handshakeCompleted = value;
	}

	public OFFeaturesReply getFeatures() {
		return features;
	}

	public long getDatapathId() {
		return datapathId;
	}

	@Override
	public void write(OFMessage m) {
		Relay.sendToCore(coreConnector, m, this.getDatapathId(), moduleHandler.getModuleId(-1, moduleName));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFMessageWriter#write(java.lang.Iterable)
	 */
	@Override
	public void write(Iterable<OFMessage> msglist) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFMessageWriter#writeRequest(org.
	 * projectfloodlight.openflow.protocol.OFRequest)
	 */
	@Override
	public <R extends OFMessage> ListenableFuture<R> writeRequest(OFRequest<R> request) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFMessageWriter#writeStatsRequest(org.
	 * projectfloodlight.openflow.protocol.OFStatsRequest)
	 */
	@Override
	public <REPLY extends OFStatsReply> ListenableFuture<List<REPLY>> writeStatsRequest(OFStatsRequest<REPLY> request) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getStatus()
	 */
	@Override
	public SwitchStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getBuffers()
	 */
	@Override
	public long getBuffers() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#disconnect()
	 */
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getActions()
	 */
	@Override
	public Set<OFActionType> getActions() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getCapabilities()
	 */
	@Override
	public Set<OFCapabilities> getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getTables()
	 */

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getSwitchDescription()
	 */
	@Override
	public SwitchDescription getSwitchDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getInetAddress()
	 */
	@Override
	public SocketAddress getInetAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getEnabledPorts()
	 */
	@Override
	public Collection<OFPortDesc> getEnabledPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getEnabledPortNumbers()
	 */
	@Override
	public Collection<OFPort> getEnabledPortNumbers() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#getPort(org.projectfloodlight.
	 * openflow.types.OFPort)
	 */
	@Override
	public OFPortDesc getPort(OFPort portNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getPort(java.lang.String)
	 */
	@Override
	public OFPortDesc getPort(String portName) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getPorts()
	 */
	@Override
	public Collection<OFPortDesc> getPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getSortedPorts()
	 */
	@Override
	public Collection<OFPortDesc> getSortedPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#portEnabled(org.projectfloodlight
	 * .openflow.types.OFPort)
	 */
	@Override
	public boolean portEnabled(OFPort portNumber) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#portEnabled(java.lang.String)
	 */
	@Override
	public boolean portEnabled(String portName) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#isConnected()
	 */
	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getConnectedSince()
	 */
	@Override
	public Date getConnectedSince() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getId()
	 */
	@Override
	public DatapathId getId() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getAttributes()
	 */
	@Override
	public Map<Object, Object> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#isActive()
	 */
	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getControllerRole()
	 */
	@Override
	public OFControllerRole getControllerRole() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#hasAttribute(java.lang.String)
	 */
	@Override
	public boolean hasAttribute(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#attributeEquals(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public boolean attributeEquals(String name, Object other) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#setAttribute(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void setAttribute(String name, Object value) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.floodlightcontroller.core.IOFSwitch#removeAttribute(java.lang.String)
	 */
	@Override
	public Object removeAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#flush()
	 */
	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getConnections()
	 */
	@Override
	public ImmutableList<IOFConnection> getConnections() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#write(org.projectfloodlight.
	 * openflow.protocol.OFMessage,
	 * net.floodlightcontroller.core.LogicalOFMessageCategory)
	 */
	@Override
	public void write(OFMessage m, LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#write(java.lang.Iterable,
	 * net.floodlightcontroller.core.LogicalOFMessageCategory)
	 */
	@Override
	public void write(Iterable<OFMessage> msglist, LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getConnectionByCategory(net.
	 * floodlightcontroller.core.LogicalOFMessageCategory)
	 */
	@Override
	public OFConnection getConnectionByCategory(LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#writeStatsRequest(org.
	 * projectfloodlight.openflow.protocol.OFStatsRequest,
	 * net.floodlightcontroller.core.LogicalOFMessageCategory)
	 */
	@Override
	public <REPLY extends OFStatsReply> ListenableFuture<List<REPLY>> writeStatsRequest(OFStatsRequest<REPLY> request,
			LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#writeRequest(org.
	 * projectfloodlight.openflow.protocol.OFRequest,
	 * net.floodlightcontroller.core.LogicalOFMessageCategory)
	 */
	@Override
	public <R extends OFMessage> ListenableFuture<R> writeRequest(OFRequest<R> request,
			LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see net.floodlightcontroller.core.IOFSwitch#getTables()
	 */
	@Override
	public short getTables() {
		// TODO Auto-generated method stub
		return 0;
	}

}
