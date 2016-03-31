/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import io.netty.buffer.Unpooled;
import org.opendaylight.netide.netiplib.HelloMessage;
import org.opendaylight.netide.netiplib.Message;
import org.opendaylight.netide.netiplib.NetIDEProtocolVersion;
import org.opendaylight.netide.netiplib.NetIPConverter;
import org.opendaylight.netide.netiplib.OpenFlowMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class ZeroMQBaseConnector implements Runnable {

    private static final String STOP_COMMAND = "Control.STOP";
    private static final String CONTROL_ADDRESS = "inproc://ShimControllerQueue";
    private NetIDEProtocolVersion netIpVersion = NetIDEProtocolVersion.VERSION_1_2;
    private static final Logger LOG = LoggerFactory.getLogger(ZeroMQBaseConnector.class);
    private String address;
    private int port;
    private ZMQ.Context context;
    private Thread thread;

    private ICoreListener coreListener;

    public ZeroMQBaseConnector() {

    }

    public void setContext(ZMQ.Context cont) {
        context = cont;
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
            // stopSocket.send(STOP_COMMAND);
            send(STOP_COMMAND, stopSocket);
            stopSocket.close();
            try {
                thread.join();
                context.term();
            } catch (InterruptedException e) {
                LOG.error("", e);
            }
        }
    }

    public boolean send(String data, ZMQ.Socket socket) {
        if (data != null && socket != null)
            socket.send(data);
        return true;
    }

    public boolean send(ZMsg message, ZMQ.Socket socket) {
        if (message != null && socket != null)
            message.send(socket);
        return true;
    }

    public void RegisterCoreListener(ICoreListener listener) {
        this.coreListener = listener;
    }

    public boolean SendData(byte[] data) {
        ZMsg msg = new ZMsg();
        msg.add(data);
        ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
        sendSocket.setIdentity("shim".getBytes());
        sendSocket.connect(CONTROL_ADDRESS);
        send(msg, sendSocket);
        sendSocket.close();
        return true;
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

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(10);
            if (poller.pollin(0)) {
                ZMsg message = ZMsg.recvMsg(socket);
                byte[] data = message.getLast().getData();
                if (coreListener != null) {
                    try{

                        Message msg = NetIPConverter.parseConcreteMessage(data);
                        if (msg.getHeader().getNetIDEProtocolVersion() == netIpVersion) {
                            if (msg instanceof HelloMessage) {

                                coreListener.onHelloCoreMessage(((HelloMessage) msg).getSupportedProtocols(),
                                        ((HelloMessage) msg).getHeader().getModuleId());
                            } else if (msg instanceof OpenFlowMessage) {

                                byte[] payload = msg.getPayload();
                                coreListener.onOpenFlowCoreMessage(msg.getHeader().getDatapathId(),
                                        Unpooled.wrappedBuffer(payload), msg.getHeader().getModuleId());
                            }
                        }
                    }catch(IllegalArgumentException ex) {
                        LOG.error("NetIp malformed message received. Message dropped.");
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
