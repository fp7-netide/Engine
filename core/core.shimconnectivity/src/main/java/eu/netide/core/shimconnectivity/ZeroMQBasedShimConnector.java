package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IConnectorListener;
import eu.netide.core.api.IShimConnector;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * Created by timvi on 09.07.2015.
 */
public class ZeroMQBasedShimConnector implements IShimConnector, Runnable {

    private int port;
    private ZMQ.Context context;
    private ZMQ.Socket socket;
    private Thread thread;
    private boolean isOpen = false;

    private IConnectorListener listener;

    public ZeroMQBasedShimConnector() {

    }

    public void Start() {
        thread = new Thread(this);
        thread.start();
    }

    public void Stop() {
        if (thread != null) {
            thread.interrupt();
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("ZeroMQBasedShimConnector stopped.");
    }

    @Override
    public void RegisterListener(IConnectorListener listener) {
        this.listener = listener;
    }

    public boolean SendData(byte[] data) {
        ZMsg msg = new ZMsg();
        msg.add("shim");
        msg.add("");
        msg.add(data);
        if (isOpen) {
            msg.send(socket);
            return true;
        }
        return false;
    }

    public void run() {
        System.out.println("ZeroMQBasedShimConnector started.");
        context = ZMQ.context(1);
        socket = context.socket(ZMQ.ROUTER);
        System.out.println("Listening for shim on port " + port);
        socket.bind("tcp://*:" + port);
        socket.setReceiveTimeOut(10);
        isOpen = true;

        ZMQ.Poller poller = new ZMQ.Poller(1);
        poller.register(socket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            int signalled = poller.poll(10);
            if (signalled == 1) {
                ZMsg message = ZMsg.recvMsg(socket);
                String senderId = message.getFirst().toString();
                byte[] data = message.getLast().getData();
                if (listener != null) {
                    listener.OnDataReceived(data);
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
        isOpen = false;
        socket.close();
        context.term();
    }

    public void setPort(int port) {
        this.port = port;
        System.out.println("ZeroMQ server port set to " + port);
    }

    public int getPort() {
        return port;
    }
}
