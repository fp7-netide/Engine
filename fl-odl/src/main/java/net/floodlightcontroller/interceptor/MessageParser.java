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

import org.json.JSONObject;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
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
		JSONObject json = new JSONObject(tmp.indexOf(",")+2);
		
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
				OFAction action = new OFAction();
				action.setType(OFActionType.OUTPUT);
				flowStats.setActions(Collections.singletonList((OFAction)action));
				flowStats.setCookie(json.getLong("cookie"));
				flowStats.setDurationNanoseconds(json.getInt("duration_nsec"));
				flowStats.setDurationSeconds(json.getInt("duration_sec"));
				flowStats.setHardTimeout(Short.parseShort(json.getString("hard_timeout")));
				flowStats.setIdleTimeout(Short.parseShort(json.getString("idle_timeout")));
				flowStats.setPacketCount(json.getLong("packet_count"));
				flowStats.setPriority(Short.parseShort(json.getString("priority")));
				flowStats.setTableId(Byte.parseByte(json.getString("table_id")));
				flowStats.setMatch(new OFMatch());
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
	
}
