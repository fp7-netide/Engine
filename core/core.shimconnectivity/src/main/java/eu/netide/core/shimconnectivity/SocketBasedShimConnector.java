package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IShimConnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by timvi on 01.07.2015.
 */
public class SocketBasedShimConnector implements IShimConnector, Runnable {

    private ServerSocket _socket;
    private Socket _client;
    private Thread _thread;

    public void Open(int port) {
        try {
            System.out.println("Starting shim socket server...");
            _socket = new ServerSocket(port);
            _thread = new Thread(this);
            _thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Close() {
        try {
            System.out.println("Closing shim connection server...");
            if (_thread != null)
                _thread.interrupt();
            if (_client != null)
                _client.close();
            _socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {

            System.out.println("Waiting for shim connection on port " + _socket.getLocalPort());
            _client = _socket.accept();
            System.out.println("Client connected (" + _client.getInetAddress().getHostAddress() + "), starting handler thread.");
            BufferedReader in = new BufferedReader(new InputStreamReader(_client.getInputStream()));

            while (in.ready()) {
                System.out.println("Shim connector received: " + in.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
