package eu.netide.core.logpub;

import java.util.HashMap;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IBackendMessageListener;
import eu.netide.core.api.IManagementMessageListener;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.lib.netip.ErrorMessage;
import eu.netide.lib.netip.ManagementMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPConverter;

public class LogPub implements IBackendMessageListener, IShimMessageListener, IManagementMessageListener, Runnable{

	private static final String STOP_COMMAND = "Control.STOP";
	private static final String CONTROL_ADDRESS = "inproc://LogPubControl";
    private static final int DEFAULT_PUB_PORT = 5557;
    private static final int DEFAULT_SUB_PORT = 5558;
    
	private static final Logger log = LoggerFactory.getLogger(LogPub.class);

	private int pubPort;
	private int subPort;

	private HashMap<HashMap<Integer,Integer>,String> hMap;

	private IShimManager shimManager;
	private IBackendManager backendManager;

	private ZMQ.Context context;
	private Thread thread;

	public LogPub() {
		log.info("LogPub Constructor.");
	}

	public void Start() {
		log.info("LogPub Start().");
		context = ZMQ.context(1);
		hMap = new HashMap<>();
		thread = new Thread(this);
		thread.setName("LogPub Receive Loop");
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
				log.error("", e);
			}
		}
		log.info("LogPub stopped.");
	}

	@Override
	public void run() {
		log.info("LogPub started.");
		ZMQ.Socket pubSocket = context.socket(ZMQ.PUB);
		pubSocket.bind("tcp://*:" + (pubPort==0?DEFAULT_PUB_PORT:pubPort));
		log.info("Listening PUB queue on port " + (pubPort==0?DEFAULT_PUB_PORT:pubPort));

		ZMQ.Socket subSocket = context.socket(ZMQ.ROUTER);
		subSocket.setIdentity("logpub".getBytes(ZMQ.CHARSET));
		subSocket.bind("tcp://*:" + (subPort==0?DEFAULT_SUB_PORT:subPort));
		log.info("Listening SUB queue on port " + (subPort==0?DEFAULT_SUB_PORT:subPort));

		ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
		controlSocket.bind(CONTROL_ADDRESS);
		log.info("Control queue on address: " + CONTROL_ADDRESS);

		// Register the queues in the poller
		try{
			ZMQ.Poller poller = new ZMQ.Poller(2);
			poller.register(subSocket, ZMQ.Poller.POLLIN);
			poller.register(controlSocket, ZMQ.Poller.POLLIN);

			while (!Thread.currentThread().isInterrupted()) {
				poller.poll(10);
				if (poller.pollin(0)) {
					ZMsg zmqMessage = ZMsg.recvMsg(subSocket);
					String dst = null;
					String src = null;
					byte[] data = null;
					try{
						ZFrame header = zmqMessage.pop();
						dst = zmqMessage.popString();
						src = zmqMessage.popString();
						data = zmqMessage.getLast().getData();
						Message netideMessage = NetIPConverter.parseRawMessage(data);
						log.debug("Data received in SUB queue: from "+src+" to "+dst+ ". (data:"+netideMessage.toByteRepresentation()+")");
						HashMap<Integer,Integer> key = new HashMap<Integer,Integer>(){{
							put(netideMessage.getHeader().getModuleId(),netideMessage.getHeader().getTransactionId());}};
							if (hMap.containsKey(key)){
								//this Tool already sent a message with this ModuleID/XID, treat as an error
								ErrorMessage error = new ErrorMessage();
								ZMsg PubMessage = new ZMsg();
								PubMessage.add(src);
								PubMessage.add(dst);
								PubMessage.add(error.toByteRepresentation());
								log.debug("Received message from "+src.substring(2)+" but it already sent a similar message (same Module_ID and XID)");
								ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
								sendSocket.connect(CONTROL_ADDRESS);
								PubMessage.send(sendSocket);
								sendSocket.close();
							}else {
								if (netideMessage.getHeader().getTransactionId() != 0)
									hMap.put(key, src);
								if (dst.startsWith("1_"))
									// send message to shim
									try{
										shimManager.sendMessage(netideMessage);
									}catch (NullPointerException e) {
										log.error("shim manager not set");
									}
								else if (dst.startsWith("0_"))
									// send message to backend
									try{
										backendManager.sendMessage(netideMessage);
									}catch (NullPointerException e) {
										log.error("backend manager not set");
									}
								else
									log.error("Got unknown message in SUB queue:" + netideMessage.toString());
							}
					}catch(NullPointerException | IllegalArgumentException e){
						log.error("Error in LogPub:");e.printStackTrace();
					}
				}
				if (poller.pollin(1)) {
					ZMsg message = ZMsg.recvMsg(controlSocket);
					if (message.getFirst().toString().equals(STOP_COMMAND)) {
						log.info("Received STOP command.\nExiting...");
						break;
					} else {
						log.debug("Sending message to PUB queue");
						message.send(pubSocket);
					}
				}
			}
		} catch (Exception ex) {
			log.error("Error in ZeroMQBasedConnector receive loop.", ex);
		} finally {
			pubSocket.close();
			subSocket.close();
			controlSocket.close();
		}
	}

	public void setPubPort(int pub_port) {
		this.pubPort = pub_port;
	}
	public int getPubPort() {
		return pubPort;
	}
	public void setSubPort(int sub_port) {
		this.subPort = sub_port;
	}
	public int getSubPort()
	{
		return subPort;
	}


	@Override
	public void OnBackendMessage(Message message, String originId) {
		log.debug("Received backend message");
		OnShimAndBackendMessage(message, "0", originId);
	}

	@Override
	public void OnShimMessage(Message message, String originId) {
		log.debug("Received shim message");
		OnShimAndBackendMessage(message, "1", originId);
	}

	private void OnShimAndBackendMessage(Message message, String origin, String originId){
		String dst = "";
		HashMap key = new HashMap<Integer,Integer>(){{put(message.getHeader().getModuleId(),message.getHeader().getTransactionId());}};
		if (hMap.containsKey(key)){
			dst = hMap.get(key);
			hMap.remove(key);
		}
		ZMsg zmq_message = new ZMsg();
		zmq_message.add(dst.isEmpty()?"2_all":dst);
		zmq_message.add(origin+"_"+originId);
		zmq_message.add(message.toByteRepresentation());
		log.debug("Received message to "+(dst.isEmpty()?"2_all":dst)+" from "+(origin.equals("0")?"backend":"shim")+":" + zmq_message.toString());
		ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
		sendSocket.connect(CONTROL_ADDRESS);
		zmq_message.send(sendSocket);
		sendSocket.close();
	}

	@Override
	public void OnManagementMessage(ManagementMessage message) {
		ZMsg zmq_message = new ZMsg();
		zmq_message.add(message.getPayloadString());
		log.debug("Received message from management:" + zmq_message.toString());
		ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
		sendSocket.connect(CONTROL_ADDRESS);
		zmq_message.send(sendSocket);
		sendSocket.close();
	}
	/**
	 * Sets the shim manager.
	 *
	 * @param manager the manager
	 */
	public void setShimManager(IShimManager manager) {
		shimManager = manager;
		log.debug("ShimManager set.");
	}

	/**
	 * Gets the shim manager.
	 *
	 * @return the shim manager
	 */
	public IShimManager getShimManager() {
		return shimManager;
	}

	/**
	 * Sets the backend manager.
	 *
	 * @param manager the manager
	 */
	public void setBackendManager(IBackendManager manager) {
		backendManager = manager;
		log.debug("BackendManager set.");
	}

	/**
	 * Gets the backend manager.
	 *
	 * @return the backend manager
	 */
	public IBackendManager getBackendManager() {
		return backendManager;
	}

	@Override
	public void OnBackendRemoved(String backEndName, LinkedList<Integer> removedModules) {
		// TODO Auto-generated method stub

	}
}