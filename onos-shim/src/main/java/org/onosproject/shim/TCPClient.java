package org.onosproject.shim;

import static org.slf4j.LoggerFactory.getLogger;

import java.awt.List;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;

import org.onosproject.shim.message.NetIDEHeader;
import org.onosproject.shim.message.NetIDEHello;
import org.onosproject.shim.message.NetIDEMessage;
import org.onosproject.shim.message.NetIDEType;
import org.slf4j.Logger;

/**

 */
public class TCPClient implements Runnable {
	private final Logger log = getLogger(getClass());

	protected Socket clientSocket = null;
	protected ShimLayer shim = null;
	protected boolean isStopped = false;
	protected Thread runningThread = null;
	protected InputStream input = null;
	protected OutputStream output = null;
	final int BUFFER_SIZE = 65536;
	

	public TCPClient(ShimLayer shim, Socket clientSocket) {
		this.clientSocket = clientSocket;
		this.shim = shim;
		try {
			input = clientSocket.getInputStream();
			output = clientSocket.getOutputStream();
		} catch (IOException e) {
			log.error("NetIDE Shim TCP Client: Error getting client streams.");
			e.printStackTrace();
		}
	}

	public void run() {
		synchronized (this) {
			this.runningThread = Thread.currentThread();
		}
		byte[] buffer = new byte[BUFFER_SIZE];
		Arrays.fill(buffer, (byte) 0);
		int totalRead = 0;
		NetIDEMessage msg = new NetIDEMessage();
		while (!isStopped()) {
			try {
				    buffer[totalRead] = (byte) input.read();
					totalRead++;
					if (totalRead == msg.getHeader().getHeaderLenght()) {
						msg.setHeader(new NetIDEHeader());
						msg.getHeader().setNetIDEVersion(buffer[0]);
						msg.getHeader().setType(buffer[1]);
						msg.getHeader().setLenght(
								ByteBuffer.wrap(buffer, 2, 3).getShort());
						msg.getHeader().setXid(
								ByteBuffer.wrap(buffer, 4, 7).getInt());
						msg.getHeader().setDatapathId(
								ByteBuffer.wrap(buffer, 8, 15).getLong());
						log.info(msg.getHeader().toString());
					}
					if (totalRead == (msg.getHeader().getHeaderLenght() + msg.getHeader().getLenght())) {
						byte[] payload = msg.getPayload();
						payload = Arrays.copyOfRange(buffer, msg.getHeader().getHeaderLenght(), 
								msg.getHeader().getHeaderLenght() + msg.getHeader().getLenght());
						msg.setPayload(payload, msg.getHeader().getLenght());
						log.info("Ecco il pacchetto che arriva completo: " + msg.toString());
						if (msg.getHeader().getType() == NetIDEType.NETIDE_HELLO) {
							NetIDEHello hello = new NetIDEHello(msg.getPayload());
							log.info(hello.toString());
							msg.setHello(hello);
							this.shim.registerClientController(this, msg);
						} else if (msg.getHeader().getType() == NetIDEType.NETIDE_OPENFLOW) {
							this.shim.processOFPacket(this, msg);
						} else {
							log.error("NetIDE Shim TCP Client: received unsupported message type");
						}

						// reset buffer
						totalRead = 0;
						Arrays.fill(buffer, (byte) 0);
						//break;
					}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	public synchronized void stop() {
		this.isStopped = true;
		try {
			this.clientSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(
					"NetIDE Shim TCP Server: Error closing client", e);
		}
	}

	public void addMessage(NetIDEMessage msg) {
		try {
			output.write(msg.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
