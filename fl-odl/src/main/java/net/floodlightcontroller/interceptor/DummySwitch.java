/**
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *     ...
 */
package net.floodlightcontroller.interceptor;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class DummySwitch implements IOFSwitch {

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#write(org.openflow.protocol.OFMessage, net.floodlightcontroller.core.FloodlightContext)
	 */
	@Override
	public void write(OFMessage m, FloodlightContext bc) throws IOException {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#write(java.util.List, net.floodlightcontroller.core.FloodlightContext)
	 */
	@Override
	public void write(List<OFMessage> msglist, FloodlightContext bc) throws IOException {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#disconnectOutputStream()
	 */
	@Override
	public void disconnectOutputStream() {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getBuffers()
	 */
	@Override
	public int getBuffers() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getActions()
	 */
	@Override
	public int getActions() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getCapabilities()
	 */
	@Override
	public int getCapabilities() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getTables()
	 */
	@Override
	public byte getTables() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#setFeaturesReply(org.openflow.protocol.OFFeaturesReply)
	 */
	@Override
	public void setFeaturesReply(OFFeaturesReply featuresReply) {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#setSwitchProperties(org.openflow.protocol.statistics.OFDescriptionStatistics)
	 */
	@Override
	public void setSwitchProperties(OFDescriptionStatistics description) {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getEnabledPorts()
	 */
	@Override
	public Collection<OFPhysicalPort> getEnabledPorts() {
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getEnabledPortNumbers()
	 */
	@Override
	public Collection<Short> getEnabledPortNumbers() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getPort(short)
	 */
	@Override
	public OFPhysicalPort getPort(short portNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getPort(java.lang.String)
	 */
	@Override
	public OFPhysicalPort getPort(String portName) {
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#setPort(org.openflow.protocol.OFPhysicalPort)
	 */
	@Override
	public void setPort(OFPhysicalPort port) {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#deletePort(short)
	 */
	@Override
	public void deletePort(short portNumber) {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#deletePort(java.lang.String)
	 */
	@Override
	public void deletePort(String portName) {
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getPorts()
	 */
	@Override
	public Collection<OFPhysicalPort> getPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#portEnabled(short)
	 */
	@Override
	public boolean portEnabled(short portName) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#portEnabled(java.lang.String)
	 */
	@Override
	public boolean portEnabled(String portName) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#portEnabled(org.openflow.protocol.OFPhysicalPort)
	 */
	@Override
	public boolean portEnabled(OFPhysicalPort port) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getId()
	 */
	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getStringId()
	 */
	@Override
	public String getStringId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getAttributes()
	 */
	@Override
	public Map<Object, Object> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getConnectedSince()
	 */
	@Override
	public Date getConnectedSince() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getNextTransactionId()
	 */
	@Override
	public int getNextTransactionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getStatistics(org.openflow.protocol.OFStatisticsRequest)
	 */
	@Override
	public Future<List<OFStatistics>> getStatistics(OFStatisticsRequest request)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getFeaturesReplyFromSwitch()
	 */
	@Override
	public Future<OFFeaturesReply> getFeaturesReplyFromSwitch()
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#deliverOFFeaturesReply(org.openflow.protocol.OFMessage)
	 */
	@Override
	public void deliverOFFeaturesReply(OFMessage reply) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#cancelFeaturesReply(int)
	 */
	@Override
	public void cancelFeaturesReply(int transactionId) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#isConnected()
	 */
	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#setConnected(boolean)
	 */
	@Override
	public void setConnected(boolean connected) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getRole()
	 */
	@Override
	public Role getRole() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#isActive()
	 */
	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#deliverStatisticsReply(org.openflow.protocol.OFMessage)
	 */
	@Override
	public void deliverStatisticsReply(OFMessage reply) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#cancelStatisticsReply(int)
	 */
	@Override
	public void cancelStatisticsReply(int transactionId) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#cancelAllStatisticsReplies()
	 */
	@Override
	public void cancelAllStatisticsReplies() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#hasAttribute(java.lang.String)
	 */
	@Override
	public boolean hasAttribute(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#setAttribute(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setAttribute(String name, Object value) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#removeAttribute(java.lang.String)
	 */
	@Override
	public Object removeAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#clearAllFlowMods()
	 */
	@Override
	public void clearAllFlowMods() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#updateBroadcastCache(java.lang.Long, java.lang.Short)
	 */
	@Override
	public boolean updateBroadcastCache(Long entry, Short port) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#getPortBroadcastHits()
	 */
	@Override
	public Map<Short, Long> getPortBroadcastHits() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#sendStatsQuery(org.openflow.protocol.OFStatisticsRequest, int, net.floodlightcontroller.core.IOFMessageListener)
	 */
	@Override
	public void sendStatsQuery(OFStatisticsRequest request, int xid,
			IOFMessageListener caller) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see net.floodlightcontroller.core.IOFSwitch#flush()
	 */
	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

}
