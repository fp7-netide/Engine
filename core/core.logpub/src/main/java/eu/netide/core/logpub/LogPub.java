package eu.netide.core.logpub;

import eu.netide.core.api.IBackendMessageListener;
import eu.netide.core.api.IManagementMessageListener;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.lib.netip.ManagementMessage;
import eu.netide.lib.netip.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.LinkedList;

public class LogPub implements IBackendMessageListener, IShimMessageListener, IManagementMessageListener, Runnable{

    private static final String STOP_COMMAND = "Control.STOP";
    private static final String CONTROL_ADDRESS = "inproc://LogPubControl";

    private static final Logger log = LoggerFactory.getLogger(LogPub.class);

    private int port;
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
        pubSocket.bind("tcp://*:" + port);
        log.info("Listening PUB queue on port " + port);

        ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
        controlSocket.bind(CONTROL_ADDRESS);
        log.info("Control queue on address: " + CONTROL_ADDRESS);

        // Register the queues in the poller
        ZMQ.Poller poller = new ZMQ.Poller(1);
        poller.register(controlSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(10);
            if (poller.pollin(0)) {
                ZMsg message = ZMsg.recvMsg(controlSocket);

                if (message.getFirst().toString().equals(STOP_COMMAND)) {
                   log.info("Received STOP command.\nExiting...");
                   break;
                } else {
                   message.send(pubSocket);
                }
            }
        }
        pubSocket.close();
        controlSocket.close();
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
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
    public void OnBackendRemoved(String backEndName, LinkedList<Integer> removedModules) {

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
        zmq_message.add(message.toByteRepresentation());
        log.debug("Received message form management:" + zmq_message.toString());
        ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
        sendSocket.connect(CONTROL_ADDRESS);
        zmq_message.send(sendSocket);
        sendSocket.close();
    }
}
