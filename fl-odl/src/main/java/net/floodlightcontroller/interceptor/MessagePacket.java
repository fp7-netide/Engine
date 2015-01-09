/**
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *     ...
 */
package net.floodlightcontroller.interceptor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketIn.OFPacketInReason;

/**
 * Used to parse a raw Packet In message into its relevant properties
 *
 * @author aleckey
 *
 */
public class MessagePacket {
	private long switchId;
	private short inPort;
	private byte[] packetData;
	private OFPacketIn packetIn;
	
	/**@return the switchId */
	public long getSwitchId() {
		return switchId;
	}

	/**@return the inPort */
	public int getInPort() {
		return inPort;
	}

	/**@return the packetData */
	public byte[] getPacketData() {
		return packetData;
	}

	/**@return the packetIn with properties set */
	public OFPacketIn getPacketIn() {
		return packetIn;
	}

	public MessagePacket() {	}

	/**
	 * Parses message into relevant properties
	 * @param rawMessage string to be parsed
	 */
	public MessagePacket(String rawMessage) {
		parseMessage(rawMessage);
	}

	/**
	 * Parses message into relevant properties
	 * @param rawMessage Assumes the following format and parses into properties:
	 * "[\"packet\", {\"raw\": [186, 100, 113, 119, ...], \"switch\": 1, \"inport\": 1}]"
	 */
	public void parseMessage(String rawMessage) {
		//EXTRACT THE JSON
		try{
		String tmp = rawMessage.substring(rawMessage.indexOf(",")+1, rawMessage.length()-1);
		JSONObject json = new JSONObject(tmp.trim());
		this.switchId = json.getLong("switch");
		this.inPort = (short)json.getInt("inport");

		JSONArray arr = json.getJSONArray("raw");
		this.packetData = new byte[arr.length()];
		for (int i=0; i<arr.length(); i++) {
			int number = (int) arr.get(i);
			this.packetData[i] = (byte)number;
		}
		}catch(Exception e){return;}
		
		//ADD PROPS TO PACKET_IN OBJECT
		packetIn = new OFPacketIn(); 
		packetIn.setInPort(inPort);
		packetIn.setPacketData(packetData);
		packetIn.setBufferId(-1);
		packetIn.setReason(OFPacketInReason.NO_MATCH);
		packetIn.setXid((int)this.switchId);
	}
	
}
