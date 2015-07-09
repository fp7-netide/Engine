package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IShimConnector;
import eu.netide.core.api.IShimMessageListener;
import org.zeromq.ZMQ;

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
        _socket.send(message);
    }

    public void run() {
        _context = ZMQ.context(1);
        _socket = _context.socket(ZMQ.ROUTER);
        System.out.println("Waiting for shim connection on port " + _port);
        _socket.bind("tcp://*:5555");

        while (!Thread.currentThread().isInterrupted()) {
            byte[] message = _socket.recv();
            System.out.println("Received " + new String(message));
            _socket.send("ack");
        }
        _socket.close();
    }
}
