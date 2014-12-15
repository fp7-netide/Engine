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
import java.util.Collections;
import java.util.List;

import net.floodlightcontroller.packet.IPv4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.OFStatisticsFactory;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFTableStatistics;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class MessageParser {

	/**
	 * Parses message into relevant properties
	 * @param rawMessage string to be parsed
	 */
	public MessageParser() { }
	
	
	/**
	 * Parses message into relevant properties
	 * @param statsType the specific Stats Type (0:DESC, 1:FLOW, 2:AGGREGATE, 3:TABLE, 4:PORT, 5:QUEUE, 6:VENDOR)
	 * @param rawMessage Assumes the following format and parses into properties:
	 * "["flow_stats_reply", 3, [{"packet_count": 0, "hard_timeout": 0, "byte_count": 0, "idle_timeout": 0, "actions": "[{'output': 65533}]", 
	 * 							  "duration_nsec": 27000000, "priority": 0, "duration_sec": 0, "table_id": 0, "cookie": 0, "match": "{}"}]]"
	 */
	public static OFStatisticsReply parseStatsReply(OFStatisticsType statsType, String rawMessage) {
		//EXTRACT THE JSON
		String tmp = rawMessage.substring(rawMessage.indexOf(",")+2, rawMessage.length()-2);
		String flagStr = tmp.substring(0, 1);
		String jsonStr = tmp.substring(tmp.indexOf(",")+3);
		JSONObject json = new JSONObject(jsonStr);
		
		//ADD PROPS TO STATS_REPLY OBJECT
		OFStatisticsReply statsReply = new OFStatisticsReply();
		statsReply.setFlags(Short.parseShort(flagStr));
		statsReply.setStatisticType(statsType);
		statsReply.setStatisticsFactory(new BasicFactory());
		List<OFStatistics> statistics = new ArrayList<OFStatistics>();
		switch (statsType) {
			case DESC:
				OFDescriptionStatistics description = new OFDescriptionStatistics();
				description.setDatapathDescription("A");
				description.setHardwareDescription("B");
				description.setManufacturerDescription("C");
				description.setSerialNumber("D");
				description.setSoftwareDescription("E");
				statistics.add(description);
				break;
			case FLOW:
				OFFlowStatisticsReply flowStats = new OFFlowStatisticsReply();
				flowStats.setByteCount(json.getLong("byte_count"));
				flowStats.setActionFactory(new BasicFactory());
				//FORMATTING SEEMS OFF (why is it in " "), NEED TO CONVERT STRING TO JSON ARRAY
				String tmpArrayStr = json.getString("actions");
				JSONArray jsonArray = new JSONArray(tmpArrayStr);
				flowStats.setActions(parseActionsArray(jsonArray));
				//
				flowStats.setCookie(json.getLong("cookie"));
				flowStats.setDurationNanoseconds(json.getInt("duration_nsec"));
				flowStats.setDurationSeconds(json.getInt("duration_sec"));
				flowStats.setHardTimeout((short)json.getInt("hard_timeout"));
				flowStats.setIdleTimeout((short)json.getInt("idle_timeout"));
				flowStats.setPacketCount(json.getLong("packet_count"));
				flowStats.setPriority((short)json.getInt("priority"));
				flowStats.setTableId((byte)json.getInt("table_id"));
				OFMatch match = new OFMatch();
				flowStats.setMatch(match);
				statistics.add(flowStats);
				break;
			case TABLE:
				OFTableStatistics tables = new OFTableStatistics();
				tables.setTableId(Byte.parseByte(json.getString("table_id")));
				//tables.setActiveCount();
				//tables.setLookupCount(lookupCount);
				//tables.setMatchedCount(matchedCount);
				//tables.setMaximumEntries(maximumEntries);
				//tables.setWildcards(wildcards);
				statistics.add(tables);
				break;
			case PORT:
				OFPortStatisticsReply portStatReply = new OFPortStatisticsReply();
				portStatReply.setreceivePackets(json.getLong("packet_count"));
				portStatReply.setPortNumber(Short.parseShort(json.getString("port")));
				portStatReply.setReceiveBytes(json.getLong("received_bytes"));
				statistics.add(portStatReply);
				break;
			case AGGREGATE:
			case VENDOR:
			case QUEUE:
		}
		statsReply.setStatistics(statistics);
		return statsReply;
	}
	
	/**
	 * Assumes input of [{'output': 65533}] and generates a list of OFActions
	 * @param array
	 * @return
	 */
	private static List<OFAction> parseActionsArray(JSONArray array) {
		List<OFAction> listActions = new ArrayList<OFAction>();
		
		for (int i=0; i<array.length(); i++) {
			JSONObject json = array.getJSONObject(i);
			String[] elementNames = JSONObject.getNames(json);
			for (String name: elementNames) {
				switch (name) {
					case "output" :
						OFActionOutput output = new OFActionOutput((short)json.getInt(name));
						listActions.add(output);
						break;
					case "dstmac":
						byte[] dlDestArr = parseByteArray(json.getJSONArray(name));
						OFActionDataLayerDestination dlDest = new OFActionDataLayerDestination(dlDestArr);
						listActions.add(dlDest);
						break;
					case "srcmac":
						byte[] dlSrcArr = parseByteArray(json.getJSONArray(name));
						OFActionDataLayerSource dlSrc = new OFActionDataLayerSource(dlSrcArr);
						listActions.add(dlSrc);
						break;
					case "dstip":
						//int dstip = fromCIDR(json.getString(name));
						int dstip = IPv4.toIPv4Address(json.getString(name));
						OFActionNetworkLayerDestination netDes = new OFActionNetworkLayerDestination(dstip);
						listActions.add(netDes);
						break;
					case "srcip":
						//int srcip = fromCIDR(json.getString(name));
						int srcip = IPv4.toIPv4Address(json.getString(name));
						OFActionNetworkLayerSource netSrc = new OFActionNetworkLayerSource(srcip);
						listActions.add(netSrc);
						break;
					case "dstport":
					case "srcport":
					case "tos":
					default:
				}
			}
		}
		return listActions;
	}
	
	private static byte[] parseByteArray(JSONArray jArray) {
		byte[] bArr = new byte[jArray.length()];
		for (int i=0; i<jArray.length(); i++) {
			int number = Integer.parseInt(jArray.get(i).toString());
			bArr[i] = (byte)number;
		}
		return bArr;
	}
	
	/**
     * Generates an integer version of the IP address
     * @param cidr "192.168.0.0/16" or "172.16.1.5"
     * @throws IllegalArgumentException
     */
    private static int fromCIDR(String cidr) throws IllegalArgumentException {
        String values[] = cidr.split("/");
        String[] ip_str = values[0].split("\\.");
        int ip = 0;
        ip += Integer.valueOf(ip_str[0]) << 24;
        ip += Integer.valueOf(ip_str[1]) << 16;
        ip += Integer.valueOf(ip_str[2]) << 8;
        ip += Integer.valueOf(ip_str[3]);
        return ip;
    }
}
