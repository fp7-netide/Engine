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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFType;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class MessageFlowStats {

	OFStatisticsReply statsReply; 
	
	/**@return the statsReply */
	public OFStatisticsReply getStatsReply() {
		return statsReply;
	}

	public MessageFlowStats() { }
	
	/**
	 * Parses message into relevant properties
	 * @param rawMessage string to be parsed
	 */
	public MessageFlowStats(String rawMessage) { 
		parseMessage(rawMessage);
	}
	
	/**
	 * Parses message into relevant properties
	 * @param rawMessage Assumes the following format and parses into properties:
	 * "["flow_stats_reply", 3, [{"packet_count": 0, "hard_timeout": 0, "byte_count": 0, "idle_timeout": 0, "actions": "[{'output': 65533}]", "duration_nsec": 27000000, "priority": 0, "duration_sec": 0, "table_id": 0, "cookie": 0, "match": "{}"}]]"
	 */
	public void parseMessage(String rawMessage) {
		//EXTRACT THE JSON
		String tmp = rawMessage.substring(rawMessage.indexOf(",")+2, rawMessage.length()-2);
		String statsType = tmp.substring(0, 1);
		JSONObject json = new JSONObject(tmp.indexOf(",")+2);
		
		//ADD PROPS TO STATS_REPLY OBJECT
		statsReply = new OFStatisticsReply();
		statsReply.setStatisticType(OFStatisticsType.valueOf(Short.parseShort(statsType) , OFType.STATS_REPLY));
		List<OFStatistics> statistics = new ArrayList<OFStatistics>();
		OFDescriptionStatistics description = new OFDescriptionStatistics();
		description.setDatapathDescription("A");
		description.setHardwareDescription("B");
		description.setManufacturerDescription("C");
		description.setSerialNumber("D");
		description.setSoftwareDescription("E");
		statistics.add(description);
		statsReply.setStatistics(statistics);
		
//		
//		this.switchId = json.getLong("switch");
//		this.inPort = (short)json.getInt("inport");
//
//		JSONArray arr = json.getJSONArray("raw");
//		this.packetData = new byte[arr.length()];
//		for (int i=0; i<arr.length(); i++) {
//			int number = Integer.parseInt(arr.get(i).toString());
//			this.packetData[i] = (byte)number;
//		}
		
	}
}
