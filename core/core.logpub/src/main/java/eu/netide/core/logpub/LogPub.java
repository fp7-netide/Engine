package eu.netide.core.logpub;

import java.lang.Runnable;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class LogPub implements Runnable{

    private static final String STOP_COMMAND = "Control.logpub.STOP";
    private static final String CONTROL_ADDRESS = "inproc://LogPubControl";
    private static final String INTERPROCESS_ADDRESS = "ipc://LogPubData";

    private int port;
    private ZMQ.Context context;
    private Thread thread;

    public LogPub() {

    }

    public void Start() {
        context = ZMQ.context(1);
        thread = new Thread(this);
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
        System.out.println("LogPub stopped.");
    }

    @Override
    public void run() {
        System.out.println("LogPub started.");

        // Publish queue for the Logger
        ZMQ.Socket pubSocket = context.socket(ZMQ.PUB);
        pubSocket.bind("tcp://*:" + port);
        System.out.println("Listening PUB queue on port " + port);

        // Pull queue for the Connector
        ZMQ.Socket inprocSocket = context.socket(ZMQ.PULL);
        inprocSocket.bind(INTERPROCESS_ADDRESS);
        System.out.println("Data queue on address: " + INTERPROCESS_ADDRESS);

        // Pull queue for control
        ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
        controlSocket.bind(CONTROL_ADDRESS);
        System.out.println("Control queue on address: " + CONTROL_ADDRESS);

        // Register the queues in the poller
        ZMQ.Poller poller = new ZMQ.Poller(2);
        poller.register(inprocSocket, ZMQ.Poller.POLLIN);
        poller.register(controlSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(10);
            if (poller.pollin(0)) {
                ZMsg message = ZMsg.recvMsg(inprocSocket);
                //System.out.println("Received message:" + message.toString());
                message.send(pubSocket); // TODO : we need to define a logic in the message format (for now [shim|backend][message])
            }
            if (poller.pollin(1)) {
                ZMsg message = ZMsg.recvMsg(controlSocket);

                if (message.getFirst().toString().equals(STOP_COMMAND)) {
                    System.out.println("Received STOP command.\nExiting...");
                    break;
                } else {
                   message.send(pubSocket); // I really don't think that should be the case
                }
            }
        }
        pubSocket.close();
        inprocSocket.close();
        controlSocket.close();
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
