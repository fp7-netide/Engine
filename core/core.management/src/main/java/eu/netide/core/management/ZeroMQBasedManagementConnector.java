package eu.netide.core.management;

import eu.netide.core.api.IConnectorListener;
import eu.netide.core.api.IManagementConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * Created by timvi on 06.08.2015.
 */
public class ZeroMQBasedManagementConnector implements IManagementConnector, Runnable {

    private static final String STOP_COMMAND = "Control.STOP";
    private static final String CONTROL_ADDRESS = "inproc://ZeroMQManagementConnectorControl";

    private static final Logger logger = LoggerFactory.getLogger(ZeroMQBasedManagementConnector.class);

    private int port;
    private ZMQ.Context context;
    private Thread thread;

    private IConnectorListener managementListener;

    public ZeroMQBasedManagementConnector() {

    }

    public void Start() {
        context = ZMQ.context(1);
        thread = new Thread(this);
        thread.setName("ZeroMQBasedManagementConnector Receive Loop");
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
                e.printStackTrace();
            }
        }
        logger.info("ZeroMQBasedManagementConnector stopped.");
    }

    @Override
    public boolean SendData(byte[] data, String destinationId) {
        ZMsg msg = new ZMsg();
        msg.add(destinationId);
        msg.add("");
        msg.add(data);
        // relayed via control socket to prevent threading issues
        ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
        sendSocket.connect(CONTROL_ADDRESS);
        msg.send(sendSocket);
        sendSocket.close();
        return true;
    }

    @Override
    public void run() {
        logger.info("ZeroMQBasedManagementConnector started.");
        ZMQ.Socket socket = context.socket(ZMQ.ROUTER);
        socket.bind("tcp://*:" + port);
        logger.info("Listening on port " + port);

        ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
        controlSocket.bind(CONTROL_ADDRESS);

        ZMQ.Poller poller = new ZMQ.Poller(2);
        poller.register(socket, ZMQ.Poller.POLLIN);
        poller.register(controlSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(10);
            if (poller.pollin(0)) {
                ZMsg message = ZMsg.recvMsg(socket);
                String senderId = message.getFirst().toString();
                byte[] data = message.getLast().getData();
                if (managementListener != null) {
                    managementListener.OnDataReceived(data, senderId);
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

    @Override
    public void RegisterManagementListener(IConnectorListener listener) {
        this.managementListener = listener;
    }
}
