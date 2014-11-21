package net.floodlightcontroller.interceptor;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFPhysicalPort;

public class MessageWorker implements Runnable {
	private List<ServerDataEvent> queue = new LinkedList<ServerDataEvent>();
	private Map<Long, DummySwitch> pendingSwitches = new HashMap<Long, DummySwitch>();
	
	public void processData(NioServer server, SocketChannel socket, byte[] data, int count) {
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		synchronized(queue) {
			queue.add(new ServerDataEvent(server, socket, dataCopy));
			queue.notify();
		}
	}
	
	public void run() {
		ServerDataEvent dataEvent;
		
		while(true) {
			// Wait for data to become available
			synchronized(queue) {
				while(queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
					}
				}
				dataEvent = (ServerDataEvent) queue.remove(0);
			}
			
			//DATA ARRIVED FROM SHIM
			String receivedMsg = new String(dataEvent.data);
			System.out.println("Msg received: " + receivedMsg);
			
			//POSSIBLE MULTIPLE MESSAGES - SPLIT
//			String[] messages = receivedMsg.split("\n");
//			for(String msg: messages) {
//				switch (getOFActionType(msg)) {
//					case SWITCH :
//						OFMessageSwitch switchMessage = new OFMessageSwitch(msg);
//						//CACHE SWITCH TILL END MESSAGE RECEIVED
//						if (switchMessage.getAction().equals("join")) {
//							if (switchMessage.getAction().equals("BEGIN")) {
//								DummySwitch newSwitch = new DummySwitch(switchMessage.getId());
//								pendingSwitches.put(switchMessage.getId(), newSwitch);
//							} else if (switchMessage.getAction().equals("END")) {
//								DummySwitch existingSwitch = pendingSwitches.get(switchMessage.getId());
//								//ADD SWITCH TO FLOODLIGHT
//								//backendChannel.addSwith(existingSwitch);
//								pendingSwitches.remove(switchMessage.getId());
//							}
//						} else {
//							//SWITCH.PART MSG
//							//backendChannel.removeSwitch(existingSwitch);
//						}
//						break;
//					case PORT :
//							OFMessagePort portMessage = new OFMessagePort(msg);
//							if (portMessage.getAction().equals("join")) {
//								//ADD THE PORT INFO TO ITS SWITCH
//								OFPhysicalPort portInfo = portMessage.getOfPort();
//								pendingSwitches.get(portMessage.getSwitchId()).setPort(portInfo);
//							} else {
//								//PART MSG
//								//backendChannel.removeSwitchPort(portMessage.getSwitchId(), portMessage.getOfPort());
//							}
//						break;
//					case PACKET :
//						
//						break;
//					default:
//						//NOT SUPPORTED YET
//				}
//			}
					
			// Return to sender
			dataEvent.data = "my response to you".getBytes();
			//dataEvent.server.send(dataEvent.socket, dataEvent.data);
		}
	}
	
	/**
	 * Assumes the following format: ["switch", "join", 1, "BEGIN"] and would returns SWITCH
	 * @param msg
	 * @return
	 */
	private OFActionType getOFActionType(String msg) {
		String tmp = msg.substring(2);
		tmp = tmp.substring(0, tmp.indexOf("\""));
		
		if (tmp.equals("switch")) return OFActionType.SWITCH;
		if (tmp.equals("packet")) return OFActionType.PACKET;
		if (tmp.equals("port")) return OFActionType.PORT;
		
		return OFActionType.UNSUPPORTED;
	}
}
