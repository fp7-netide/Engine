package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IShimConnector;
import eu.netide.core.api.IShimMessageListener;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * Created by timvi on 09.07.2015.
 */
public class ZeroMQBasedShimConnector implements IShimConnector, Runnable {

    private int _port;
    private ZMQ.Context _context;
    private ZMQ.Socket _socket;
    private Thread _thread;
    private boolean _isOpen = false;

    private IShimMessageListener _listener;

    public ZeroMQBasedShimConnector() {

    }

    public void Start() {
        _thread = new Thread(this);
        _thread.start();
    }

    public void Stop() {
        if (_thread != null) {
            _thread.interrupt();
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("ZeroMQBasedShimConnector stopped.");
    }

    public void SendMessage(String message) {
        System.out.println("Message for sending to shim: " + message);
        ZMsg msg = new ZMsg();
        msg.add("PS1"); // TODO replace with shim id
        msg.add("");
        msg.add(message);
        if (_isOpen)
            msg.send(_socket);
    }

    public void run() {
        System.out.println("ZeroMQBasedShimConnector started.");
        _context = ZMQ.context(1);
        _socket = _context.socket(ZMQ.ROUTER);
        System.out.println("Listening for shim on port " + _port);
        _socket.bind("tcp://*:" + _port);
        _socket.setReceiveTimeOut(10);
        _isOpen = true;

        ZMQ.Poller poller = new ZMQ.Poller(1);
        poller.register(_socket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            int signalled = poller.poll(10);
            if (signalled == 1) {
                ZMsg message = ZMsg.recvMsg(_socket);
                String senderId = message.getFirst().toString();
                String messageText = message.getLast().toString();
                System.out.println("Received from '" + senderId + "':" + messageText);
                ZMsg response = new ZMsg();
                response.add(senderId);
                response.add("Pingback. You sent '" + messageText + "'.");
                response.send(_socket);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
        _isOpen = false;
        _socket.close();
        _context.term();
    }

    public void setPort(int port) {
        _port = port;
        System.out.println("ZeroMQ server port set to" + port);
    }

    public int getPort() {
        return _port;
    }
}
