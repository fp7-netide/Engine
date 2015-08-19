package eu.netide.core.logpub;

import java.lang.Runnable;

import eu.netide.core.api.IBackendMessageListener;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.lib.netip.Message;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class LogPub implements IBackendMessageListener, IShimMessageListener,Runnable{

    private static final String STOP_COMMAND = "Control.logpub.STOP";
    private static final String CONTROL_ADDRESS = "inproc://LogPubControl";

    private int port;
    private ZMQ.Context context;
    private Thread thread;
    ZMQ.Socket pubSocket;
    ZMQ.Socket controlSocket;

    public LogPub() {

    }

    public void Start() {
        context = ZMQ.context(1);
        pubSocket = context.socket(ZMQ.PUB);
        controlSocket = context.socket(ZMQ.PULL);
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

        pubSocket.bind("tcp://*:" + port);
        System.out.println("Listening PUB queue on port " + port);

        controlSocket.bind(CONTROL_ADDRESS);
        System.out.println("Control queue on address: " + CONTROL_ADDRESS);

        // Register the queues in the poller
        ZMQ.Poller poller = new ZMQ.Poller(1);
        poller.register(controlSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(10);
            if (poller.pollin(0)) {
                ZMsg message = ZMsg.recvMsg(controlSocket);

                if (message.getFirst().toString().equals(STOP_COMMAND)) {
                    System.out.println("Received STOP command.\nExiting...");
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
        ZMsg Zmessage = new ZMsg();
        Zmessage.add("backend");
        Zmessage.add(originId);
        Zmessage.add(message.getHeader().toByteRepresentation());
        Zmessage.add(message.getPayload());
        System.out.println("Received message:" + Zmessage.toString());
        Zmessage.send(pubSocket);
    }

    @Override
    public void OnShimMessage(Message message) {
        ZMsg Zmessage = new ZMsg();
        Zmessage.add("shim");
        Zmessage.add("");
        Zmessage.add(message.getHeader().toByteRepresentation());
        Zmessage.add(message.getPayload());
        System.out.println("Received message:" + Zmessage.toString());
        Zmessage.send(pubSocket);
    }

}
