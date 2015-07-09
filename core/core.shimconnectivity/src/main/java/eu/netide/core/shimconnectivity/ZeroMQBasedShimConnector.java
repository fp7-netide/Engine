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

    private IShimMessageListener _listener;

    public ZeroMQBasedShimConnector(IShimMessageListener listener) {
        _listener = listener;
    }

    public void Open(int port) {
        System.out.println("Starting shim ZeroMQ server...");
        _port = port;
        _thread = new Thread(this);
        _thread.start();
    }

    public void Close() {
        System.out.println("Closing shim connection server...");
        if (_thread != null) {
            _thread.interrupt();
        }
        _context.term();
    }

    public void SendMessage(String message) {
        System.out.println("Message for sending to shim: " + message);
        ZMsg msg = new ZMsg();
        msg.add("PS1"); // TODO replace with shim id
        msg.add("");
        msg.add(message);
        msg.send(_socket);
    }

    public void run() {
        _context = ZMQ.context(1);
        _socket = _context.socket(ZMQ.ROUTER);
        System.out.println("Listening for shim on port " + _port);
        _socket.bind("tcp://*:5555");

        while (!Thread.currentThread().isInterrupted()) {
            ZMsg message = ZMsg.recvMsg(_socket);
            String senderId = message.getFirst().toString();
            String messageText = message.getLast().toString();
            System.out.println("Received from '" + senderId + "':" + messageText);
            ZMsg response = new ZMsg();
            response.add(senderId);
            response.add("Pingback. You sent '" + messageText + "'.");
            response.send(_socket);
        }
        _socket.close();
    }
}
