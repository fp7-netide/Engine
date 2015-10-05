package org.onosproject.shim;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;

public class TCPServer implements Runnable {
	private final Logger log = getLogger(getClass());

	protected int serverPort;
	protected ServerSocket tcpServer = null;
	protected boolean isStopped = false;
	protected Thread runningThread = null;
	private ShimLayer shim;

	public TCPServer(ShimLayer shim, int tcpPort) {
		this.shim = shim;
		this.serverPort = tcpPort;
		openServerSocket();
	}

	@Override
	public void run() {
		synchronized (this) {
			this.runningThread = Thread.currentThread();
		}
		while (!isStopped()) {
			Socket clientSocket = null;
			try {
				clientSocket = this.tcpServer.accept();
			} catch (IOException e) {
				if (isStopped()) {
					log.error("NetIDE Shim TCP Server Stopped.");
					return;
				}
				throw new RuntimeException(
						"NetIDE Shim TCP Server: Error accepting client connection",
						e);
			}
			log.info("NetIDE Shim TCP Server: New client connected!");
			new Thread(new TCPClient(shim, clientSocket))
					.start();
		}
	}

	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	public synchronized void stop() {
		this.isStopped = true;
		try {
			this.tcpServer.close();
		} catch (IOException e) {
			throw new RuntimeException("NetIDE Shim TCP Server: Error closing server", e);
		}
	}
	
    private void openServerSocket() {
        try {
            this.tcpServer = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("NetIDE Shim TCP Server: Cannot open tcp server port", e);
        }
    }

}
