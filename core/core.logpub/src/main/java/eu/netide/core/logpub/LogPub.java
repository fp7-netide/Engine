package eu.netide.core.logpub;

import eu.netide.core.api.*;
import eu.netide.lib.netip.ManagementMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageHeader;
import eu.netide.lib.netip.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class LogPub implements IBackendMessageListener, IShimMessageListener, IManagementMessageListener, Runnable{

    private static final String STOP_COMMAND = "Control.STOP";
    private static final String CONTROL_ADDRESS = "inproc://LogPubControl";

    private static final Logger log = LoggerFactory.getLogger(LogPub.class);

    private int pub_port;
    private int sub_port;

    private ZMQ.Context context;
    private Thread thread;

    public LogPub() {
        log.info("LogPub Constructor.");
    }

    public void Start() {
        log.info("LogPub Start().");
        context = ZMQ.context(1);
        thread = new Thread(this);
        thread.setName("LogPub Receive Loop");
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
                log.error("", e);
            }
        }
        log.info("LogPub stopped.");
    }

    @Override
    public void run() {
        log.info("LogPub started.");
        ZMQ.Socket pubSocket = context.socket(ZMQ.PUB);
        pubSocket.bind("tcp://*:" + pub_port);
        log.info("Listening PUB queue on port " + pub_port);

        ZMQ.Socket subSocket = context.socket(ZMQ.ROUTER);
        subSocket.setIdentity("logpub".getBytes(ZMQ.CHARSET));
        subSocket.bind("tcp://*:" + sub_port);
        log.info("Listening SUB queue on port " + sub_port);

        ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
        controlSocket.bind(CONTROL_ADDRESS);
        log.info("Control queue on address: " + CONTROL_ADDRESS);

        // Register the queues in the poller
        ZMQ.Poller poller = new ZMQ.Poller(2);
        poller.register(subSocket, ZMQ.Poller.POLLIN);
        poller.register(controlSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(10);
            if (poller.pollin(0)) {
                ZMsg message = ZMsg.recvMsg(subSocket);
                String senderId = message.getFirst().toString();
                byte[] data = message.getLast().getData();
                log.info("Data received from SUB queue to'" + senderId + "'.");
                if (senderId.equals("1_")) {
                    // send all messages to shim
                    Message shim_message = new Message(new MessageHeader(),data);
                    shim_message.getHeader().setMessageType(MessageType.OPENFLOW);

                } else if (senderId.equals("0_")) {
                    // send all messages to backend
                    Message be_message = new Message(new MessageHeader(),data);
                    be_message.getHeader().setMessageType(MessageType.OPENFLOW);

                } else{
                    log.debug("Got unknown message in SUB queue:" + message.toString());
                }
            }
            if (poller.pollin(1)) {
                ZMsg message = ZMsg.recvMsg(controlSocket);
                if (message.getFirst().toString().equals(STOP_COMMAND)) {
                   log.info("Received STOP command.\nExiting...");
                   break;
                } else {
                    log.debug("Sending message to PUB queue");
                    message.send(pubSocket);

                }
            }
        }
        pubSocket.close();
        controlSocket.close();
    }

    public void setPubPort(int pub_port) {
        this.pub_port = pub_port;
    }
    public int getPubPort() {
        return pub_port;
    }
    public void setSubPort(int sub_port) {
        this.sub_port = sub_port;
    }
    public int getSubPort()
    {
        return sub_port;
    }


    @Override
    public void OnBackendMessage(Message message, String originId) {
        ZMsg zmq_message = new ZMsg();
        zmq_message.add("0_" + originId);
        zmq_message.add(message.toByteRepresentation());
        log.debug("Received message from backend:" + zmq_message.toString());
        ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
        sendSocket.connect(CONTROL_ADDRESS);
        zmq_message.send(sendSocket);
        sendSocket.close();
    }

    @Override
    public void OnShimMessage(Message message, String originId) {
        ZMsg zmq_message = new ZMsg();
        zmq_message.add("1_" + originId);
        zmq_message.add(message.toByteRepresentation());
        log.debug("Received message form shim:" + zmq_message.toString());
        ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
        sendSocket.connect(CONTROL_ADDRESS);
        zmq_message.send(sendSocket);
        sendSocket.close();
    }


    @Override
    public void OnManagementMessage(ManagementMessage message) {
        ZMsg zmq_message = new ZMsg();
        zmq_message.add(message.getPayloadString());
        log.debug("Received message form management:" + zmq_message.toString());
        ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
        sendSocket.connect(CONTROL_ADDRESS);
        zmq_message.send(sendSocket);
        sendSocket.close();
    }
}
