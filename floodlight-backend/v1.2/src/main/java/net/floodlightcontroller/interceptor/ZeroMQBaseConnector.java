/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.ModuleAcknowledgeMessage;
import eu.netide.lib.netip.NetIPConverter;
import eu.netide.lib.netip.OpenFlowMessage;
import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import java.util.List;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ZeroMQBaseConnector implements Runnable {
	private static final String STOP_COMMAND = "Control.STOP";
	private static final String CONTROL_ADDRESS = "inproc://BackendControllerQueue";

	private static final Logger LOG = LoggerFactory.getLogger(ZeroMQBaseConnector.class);
	private String address;
	private int port;
	private ZMQ.Context context;
	private Thread thread;

	private ICoreListener coreListener;
	private IModuleHandler moduleListener;
	List<Pair<Protocol, ProtocolVersions>> supportedProtocols;

	public ZeroMQBaseConnector(List<Pair<Protocol, ProtocolVersions>> supportedProtocols) {
		this.supportedProtocols = supportedProtocols;
	}

	public void Start() {
		context = ZMQ.context(1);
		thread = new Thread(this);
		thread.setName("ZeroMQBasedConnector Receive Loop");
		thread.start();
	}

	public void Stop() {
		if (thread != null) {
			ZMQ.Socket stopSocket = context.socket(ZMQ.PUSH);
			stopSocket.connect(CONTROL_ADDRESS);
			stopSocket.send(STOP_COMMAND);
			stopSocket.close();
			try {
				thread.join();
				context.term();
			} catch (InterruptedException e) {
				LOG.error("", e);
			}
		}
	}

	public void RegisterCoreListener(ICoreListener listener) {
		this.coreListener = listener;
	}

	public void RegisterModuleListener(IModuleHandler listener) {
		this.moduleListener = listener;
	}

	public boolean SendData(byte[] data) {
		ZMsg msg = new ZMsg();
		msg.add(data);
		if (context != null) {
			ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
			sendSocket.setIdentity("backend".getBytes());
			sendSocket.connect(CONTROL_ADDRESS);
			msg.send(sendSocket);
			sendSocket.close();
			return true;
		}
		LOG.info("Core context null");
		return false;

	}

	@Override
	public void run() {
		ZMQ.Socket socket = context.socket(ZMQ.DEALER);
		socket.setIdentity("floodlight-backend".getBytes());
		socket.connect("tcp://" + getAddress() + ":" + getPort());
		LOG.info("Trying to connect to core on address tcp://" + getAddress() + ":" + getPort());

		ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
		controlSocket.bind(CONTROL_ADDRESS);

		ZMQ.Poller poller = new ZMQ.Poller(2);
		poller.register(socket, ZMQ.Poller.POLLIN);
		poller.register(controlSocket, ZMQ.Poller.POLLIN);
		LOG.info("Successfully connected to core on address tcp://" + getAddress() + ":" + getPort());

		while (!Thread.currentThread().isInterrupted()) {
			poller.poll(10);
			if (poller.pollin(0)) {
				ZMsg message = ZMsg.recvMsg(socket);
				byte[] data = message.getLast().getData();
				if (coreListener != null) {
					Message msg = NetIPConverter.parseConcreteMessage(data);
					if (msg instanceof HelloMessage) {
						coreListener.onHelloCoreMessage(((HelloMessage) msg).getSupportedProtocols(),
								((HelloMessage) msg).getHeader().getModuleId());
					} else if (msg instanceof OpenFlowMessage) {
						coreListener.onOpenFlowCoreMessage(msg.getHeader().getDatapathId(),
								((OpenFlowMessage) msg).getOfMessage(), msg.getHeader().getModuleId(), msg.getHeader().getTransactionId());
					} else if (msg instanceof ModuleAcknowledgeMessage) {
						int moduleId = ((ModuleAcknowledgeMessage) msg).getHeader().getModuleId();
						String moduleName = ((ModuleAcknowledgeMessage) msg).getModuleName();
						LOG.info("Module Announcement received. ModuleName: " + moduleName + " ModuleId: " + moduleId);
						moduleListener.onModuleAckMessage(moduleName, moduleId);
					}
				}
			}
			if (poller.pollin(1)) {
				ZMsg message = ZMsg.recvMsg(controlSocket);
				if (message.getFirst().toString().equals(STOP_COMMAND)) {
					break;
				} else {
					message.send(socket);
				}
			}
		}
		socket.close();
		controlSocket.close();
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

}
