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
public class OFMessagePacket {
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

	public OFMessagePacket() {	}

	/**
	 * Parses message into relevant properties
	 * @param rawMessage string to be parsed
	 */
	public OFMessagePacket(String rawMessage) {
		parseMessage(rawMessage);
	}

	/**
	 * Parses message into relevant properties
	 * @param rawMessage Assumes the following format and parses into properties:
	 * "[\"packet\", {\"raw\": [186, 100, 113, 119, 229, 86, 190, 234, 9, 83, 209, 248, 8, 0, 69, 0, 0, 84, 110, 53, 0, 0, 64, 1, 248, 113, 10, 0, 0, 2, 10, 0, 0, 1, 0, 0, 157, 245, 111, 210, 0, 4, 13, 76, 53, 84, 190, 144, 6, 0, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55], \"switch\": 1, \"inport\": 1}]"
	 */
	public void parseMessage(String rawMessage) {
		//EXTRACT THE JSON
		String tmp = rawMessage.substring(rawMessage.indexOf(",")+1, rawMessage.length()-1);
		JSONObject json = new JSONObject(tmp.trim());
		this.switchId = json.getLong("switch");
		this.inPort = (short)json.getInt("inport");

		JSONArray arr = json.getJSONArray("raw");
		this.packetData = new byte[arr.length()];
		for (int i=0; i<arr.length(); i++) {
			int number = Integer.parseInt(arr.get(i).toString());
			this.packetData[i] = (byte)number;
		}
		
		//ADD PROPS TO PACKET_IN OBJECT
		packetIn = new OFPacketIn(); 
		packetIn.setInPort(inPort);
		packetIn.setPacketData(packetData);
		packetIn.setBufferId(inPort);
		packetIn.setReason(OFPacketInReason.NO_MATCH);
		packetIn.setXid((int)this.switchId);
	}
	
}
