/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.shim;

import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.ModuleAnnouncementMessage;
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
 * @author kevin.phemius@thalesgroup.com
 * @author giuseppex.petralia@intel.com
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
    List<Pair<Protocol, ProtocolVersions>> supportedProtocols;
    
    public ZeroMQBaseConnector(String netideCoreAddress, int netideCorePort, Shim shim) {
    	this.address = netideCoreAddress;
    	this.port = netideCorePort;
    	registerCoreListener(shim);
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

    public void registerCoreListener(ICoreListener listener) {
        this.coreListener = listener;
    }

    public boolean sendData(byte[] data) {
        ZMsg msg = new ZMsg();
        msg.add(data);
        if (context != null) {
            ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
            sendSocket.setIdentity("shim".getBytes());
            sendSocket.connect(CONTROL_ADDRESS);
            msg.send(sendSocket);
            sendSocket.close();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        ZMQ.Socket socket = context.socket(ZMQ.DEALER);
        socket.setIdentity("shim".getBytes());
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
                    LOG.info("Received message from Core:"+msg.toString());
                    if (msg instanceof HelloMessage) {
                        coreListener.onHelloCoreMessage(((HelloMessage) msg));
                    } else if (msg instanceof OpenFlowMessage) {
                        coreListener.onOpenFlowCoreMessage(((OpenFlowMessage) msg));
                    } else if (msg instanceof ModuleAnnouncementMessage) {
                    	coreListener.onModuleAnnouncementMessage((ModuleAnnouncementMessage) msg);
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

    public int getPort() {
        return port;
    }
    public String getAddress() {
        return address;
    }

}
