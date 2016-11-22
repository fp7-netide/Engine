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

import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.ModuleAcknowledgeMessage;
import eu.netide.lib.netip.NetIPConverter;
import eu.netide.lib.netip.OpenFlowMessage;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by antonio on 05/02/16.
 */
public class ZeroMQBaseConnector implements Runnable {


    private static final String STOP_COMMAND = "Control.STOP";
    private static final String CONTROL_ADDRESS = "inproc://BackendControllerQueue";

    private final Logger log = getLogger(getClass());
    private String address;
    private int port;
    private ZMQ.Context context;
    private Thread thread;

    private ICoreListener coreListener;
    private IModuleHandler moduleListener;

    public ZeroMQBaseConnector() {

    }

    public void setContext(ZMQ.Context cont) {
        context = cont;
    }

    public void Start() {
        context = ZMQ.context(1);
        thread = new Thread(this);
        thread.setName("ZeroMQBasedConnector Receive Loop Backend");
        thread.start();
    }

    public void Stop() {
        if (thread != null) {
            ZMQ.Socket stopSocket = context.socket(ZMQ.PUSH);
            stopSocket.connect(CONTROL_ADDRESS);
            // stopSocket.send(STOP_COMMAND);
            send(STOP_COMMAND, stopSocket);
            stopSocket.close();
            try {
                thread.join();
                context.term();
            } catch (InterruptedException e) {
                log.error("", e);
            }
            log.info("ZMQ Socket closed");
        }
    }

    public boolean send(String data, ZMQ.Socket socket) {
        if (data != null && socket != null)
            return socket.send(data);
        return false;
    }

    public boolean send(ZMsg message, ZMQ.Socket socket) {
        if (message != null && socket != null)
            return message.send(socket);
        return false;
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
            sendSocket.setIdentity(BackendLayer.MODULE_NAME.getBytes());
            sendSocket.connect(CONTROL_ADDRESS);
            boolean test = false;
            test = send(msg, sendSocket);
            sendSocket.close();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        ZMQ.Socket socket = context.socket(ZMQ.DEALER);
        socket.setIdentity(BackendLayer.MODULE_NAME.getBytes());
        socket.connect("tcp://" + getAddress() + ":" + getPort());
        log.info("Trying to connect to core on address tcp://" + getAddress() + ":" + getPort());

        ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
        controlSocket.bind(CONTROL_ADDRESS);

        ZMQ.Poller poller = new ZMQ.Poller(2);
        poller.register(socket, ZMQ.Poller.POLLIN);
        poller.register(controlSocket, ZMQ.Poller.POLLIN);

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

                        byte[] payload = msg.getPayload();
                        coreListener.onOpenFlowCoreMessage(msg.getHeader().getDatapathId(),
                                                           Unpooled.wrappedBuffer(payload), msg.getHeader().getModuleId(),
                                                           msg.getHeader().getTransactionId());
                    } else if (msg instanceof ModuleAcknowledgeMessage) {
                        int moduleId = ((ModuleAcknowledgeMessage) msg).getHeader().getModuleId();
                        String moduleName = ((ModuleAcknowledgeMessage) msg).getModuleName();
                        log.info("Module Announcement received. ModuleName: " + moduleName + " ModuleId: " + moduleId);
                        moduleListener.onModuleAckMessage(moduleName, moduleId);
                    } else {
                        // LOG.info("Core Unrecognized Message received class
                        // {}, header: {}", msg.getClass(),
                        // msg.getHeader().getMessageType());
                    }
                }
            }
            if (poller.pollin(1)) {
                ZMsg message = ZMsg.recvMsg(controlSocket);
                if (message.getFirst().toString().equals(STOP_COMMAND)) {
                    log.info("Received STOP command. Exiting...");
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
