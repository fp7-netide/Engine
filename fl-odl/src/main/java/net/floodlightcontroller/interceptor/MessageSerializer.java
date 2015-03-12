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

import net.floodlightcontroller.packet.IPv4;

import org.json.JSONObject;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionNetworkTypeOfService;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionTransportLayer;
import org.openflow.protocol.action.OFActionTransportLayerDestination;
import org.openflow.protocol.action.OFActionTransportLayerSource;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class MessageSerializer {

	public MessageSerializer() { }
	
	/**
	 * Serializes an Openflow Statistics Request message for Pyretic
	 * @param switchID switch id
	 * @param statsRequest object to be serialized
	 * @return
	 */
	public static String serializeMessage(long switchID, OFStatisticsRequest statsRequest) {
		String retString="";
		switch (statsRequest.getStatisticType()) {
			case DESC: 
				break;
			case FLOW:
				retString = "[\"flow_stats_request\", " + switchID + "]" + "\n";
				break;
			case AGGREGATE:
			case PORT:
			case TABLE:
			case VENDOR:
			case QUEUE:
		}
		return retString;
	}
	/**
	 * byte in Java is in the range [-127, 127]. Pyretic can only process values that
	 * are in the range [0, 255]. This functions converts the array from one format to
	 * the other.
	 * @param raw byte[]
	 * @return ArrayList<Integer>
	 */
	public static ArrayList<Integer> ByteArrayConvert(byte[] raw){
		ArrayList<Integer> new_raw = new ArrayList<Integer>();
		for (byte x: raw){
			int y =  (x & 0xff);
			new_raw.add(y);	
		};
		return new_raw;
	}
	
	public static int[] convert_mac(byte[] raw) {
		StringBuilder sb = new StringBuilder(18);
		    for (byte b : raw) {
		        if (sb.length() > 0)
		            sb.append(':');
		        sb.append(String.format("%02x", b));
		    }
		    int len = sb.toString().toCharArray().length;
		    int[] convert = new int[len];
		    for (int i =0; i<len; i++) 
		    	convert[i] = (int) sb.toString().toCharArray()[i];
		    return convert;
	}
	
	public static int[] convert_ip (int bytes) {
		StringBuffer to_return = new StringBuffer();
		to_return.append((byte)((bytes >>> 24) & 0xff)).append(".").append((byte)((bytes >>> 16) & 0xff)).
		append(".").append((byte)((bytes >>>  8) & 0xff)).append(".").append((byte)((bytes       ) & 0xff));
		int len = to_return.toString().toCharArray().length;
	    int[] convert = new int[len];
	    for (int i =0; i<len; i++)
	    	convert[i] = (int) to_return.toString().toCharArray()[i];
	    return convert;
	}
	/**
	 * Serializes an Openflow OFPacketOut message for Pyretic
	 * @param switchID switch id
	 * @param packetOut object to be serialized
	 * @return
	 */
	public static String serializeMessage(long switchID, OFPacketOut packetOut) {
		//["packet", {"outport": 1, "protocol": 2, "header_len": 14, "inport": 2, 
		// "dstip": [49, 48, 46, 48, 46, 48, 46, 49], 
		// "srcmac": [99, 101, 58, 97, 56, 58, 100, 100, 58, 99, 102, 58, 49, 99, 58, 97, 101], "dstmac": [99, 101, 58, 97, 54, 58, 99, 51, 58, 100, 100, 58, 56, 57, 58, 99, 51], 
		// "raw": [206, 166, 195, 221, 137, 195, 206, 168, 221, 207, 28, 174, 8, 6, 0, 1, 8, 0, 6, 4, 0, 2, 206, 168, 221, 207, 28, 174, 10, 0, 0, 2, 206, 166, 195, 221, 137, 195, 10, 0, 0, 1], 
		// "payload_len": 42, "switch": 1, "ethtype": 2054, "srcip": [49, 48, 46, 48, 46, 48, 46, 50] }] + TERM_CHAR  
		JSONObject json = new JSONObject(); 
		json.put("switch", switchID); 
		json.put("buf",  packetOut.getBufferId());
		json.put("inport",  packetOut.getInPort());
	    byte[] raw =  packetOut.getPacketData();

	    // The array that we need to pass to pyretic must have unsigned values 
	    // so we need to change the range of the array returned by getPacketData (range -127, 127) to
	    // (0,255). Otherwise pyretic will not be able to read the of packet's payload. 
	    ArrayList<Integer> new_raw = ByteArrayConvert(raw); 
		json.put("raw",  new_raw);
		json.put("header_len", packetOut.getActionsLength());
		JSONObject[] actionArray = new JSONObject[packetOut.getActions().size()];
		for (int i=0; i<packetOut.getActions().size(); i++) {
			actionArray[i] = getAction(packetOut.getActions().get(i));
			String key = JSONObject.getNames(actionArray[i])[0];
			Object value = actionArray[i].get(key);
			json.put(key, value);
		}
		json.put("actions", actionArray);
		
		StringBuilder sb = new StringBuilder("[\"packet\", ");
		sb.append(json.toString());
		sb.append("]\n");
		return sb.toString();
	}

	/**
	 * Serializes an Openflow OFFlowMod message for Pyretic
	 * @param switchID switch id
	 * @param OFFlowMod object to be serialized
	 * @return
	 */
	public static String serializeMessage(long switchID, OFFlowMod flowMod) {
		//["install",  
		//  {"dstip": [49, 48, 46, 48, 46, 48, 46, 49], "srcip": [49, 48, 46, 48, 46, 48, 46, 50],
		//   "dstmac": [54, 97, 58, 50, 100, 58, 55, 102, 58, 50, 57, 58, 99, 56, 58, 54, 49], "srcmac": [49, 50, 58, 57, 51, 58, 50, 99, 58, 52, 97, 58, 52, 56, 58, 50, 52],
		//   "dstport": 0, "srcport": 0
		//   "protocol": 1,  "tos": 0, "inport": 1, "switch": 2, "ethtype": 2048}, 0, [{"outport": 2}],]
		
		JSONObject json = new JSONObject();
		// Configure match fields in the flowmod message
		OFMatch match = flowMod.getMatch();
		System.out.println("match is " + match.toString());
		if ((match.getWildcards() & OFMatch.OFPFW_DL_TYPE) == 0)
			json.put("ethtype",flowMod.getMatch().getDataLayerType());
		
		if ((match.getWildcards() & OFMatch.OFPFW_DL_DST) == 0)
			//Use this line with POX
			//json.put("dstmac", ByteArrayConvert(match.getDataLayerDestination()));
			//Use this line with Ryu
			json.put("dstmac", convert_mac(match.getDataLayerDestination()));
		
		if ((match.getWildcards() & OFMatch.OFPFW_DL_SRC) == 0)
			//Use this line with POX
			//json.put("srcmac", ByteArrayConvert(match.getDataLayerSource()));
			//Use this line with Ryu
			json.put("srcmac", convert_mac(match.getDataLayerSource()));
		
		if ((match.getWildcards() & OFMatch.OFPFW_NW_DST_BITS) == 0 )
			//Use this line with POX
			//json.put("dstip", match.getNetworkDestination());
			//Use this line with Ryu
			json.put("dstip", convert_ip(match.getNetworkDestination()));		
		if ((match.getWildcards() & OFMatch.OFPFW_NW_SRC_BITS) == 0 )
			//Use this line with POX
			//json.put("srcip", match.getNetworkSource());
			//Use this line with Ryu
			json.put("srcip", convert_ip(match.getNetworkSource()));
		if ((match.getWildcards() & OFMatch.OFPFW_TP_DST) == 0) 
			json.put("dstport", 0x0FFFF & match.getTransportDestination());
		
		if ((match.getWildcards() & OFMatch.OFPFW_TP_SRC) == 0)
			json.put("srcport", match.getTransportSource());
		
		if ((match.getWildcards() & OFMatch.OFPFW_NW_TOS) == 0)
			json.put("tos", (match.getNetworkTypeOfService() & 0xff));
		
		if ((match.getWildcards() & OFMatch.OFPFW_IN_PORT) == 0)
			json.put("inport", flowMod.getMatch().getInputPort());
		
		if ((match.getWildcards() & OFMatch.OFPFW_NW_PROTO) == 0)
			json.put("protocol", flowMod.getMatch().getNetworkProtocol());
		
		if ((match.getWildcards() & OFMatch.OFPFW_DL_VLAN) == 0)
			if (match.getDataLayerVirtualLan() != -1 )
				json.put("vlan_id", match.getDataLayerVirtualLan());
		if ((match.getWildcards() & OFMatch.OFPFW_DL_VLAN_PCP) == 0)
			json.put("vlan_pcp", match.getDataLayerVirtualLanPriorityCodePoint());
		json.put("switch", switchID);
		json.put("outPort", flowMod.getOutPort());
		StringBuilder sb = new StringBuilder("[\"");
		
		//COMMAND
		switch (flowMod.getCommand()) {
			case OFFlowMod.OFPFC_ADD: 
				sb.append("install"); 
				break;
			case OFFlowMod.OFPFC_DELETE: 
				sb.append("delete"); 
				break;
			case OFFlowMod.OFPFC_MODIFY: 
				sb.append("modify"); 
				break;
			default:
		}
		sb.append("\", ");
		
		//MATCH
		sb.append(json.toString());
		
		//PRIORITY
		sb.append(", ").append("65000");  //    flowMod.getPriority() /* 100*/
		
		//ACTION LIST
		if (flowMod.getActions().size() == 0){
			sb.append("]\n");
		} else {
		int action_list_length = flowMod.getActions().size();	
		for (int i=0; i<action_list_length; i++) {
			sb.append(", [{");
			OFAction action = flowMod.getActions().get(i);
			switch (action.getType()) {
			case OUTPUT:
				sb.append("\"outport\": ")
			    	.append((0x0FFFF&(((OFActionOutput)action).getPort())));
				if (i != action_list_length-1)
					sb.append(", ");
				break;
			case SET_DL_DST:
				sb.append("\"dstmac\": ")
		    		.append(((OFActionDataLayerDestination)action).getDataLayerAddress());
				if (i != action_list_length-1)
					sb.append(", ");
				break;
			case SET_DL_SRC:
				sb.append("\"srcmac\": ")
	    			.append(((OFActionDataLayerSource)action).getDataLayerAddress());
				if (i != action_list_length-1)
					sb.append(", ");
				break;
			case SET_NW_DST:
				sb.append("\"dstip\": ")
	    			.append(((OFActionNetworkLayerDestination)action).getNetworkAddress());
				if (i != action_list_length-1)
				sb.append(", ");
				break;
			case SET_NW_SRC:
				sb.append("\"srcip\": ")
    				.append(((OFActionNetworkLayerSource)action).getNetworkAddress());
			if (i != action_list_length-1)
			sb.append(", ");
				break;
			case SET_NW_TOS:
				sb.append("\"tos\": ")
					.append(((OFActionNetworkTypeOfService)action).getNetworkTypeOfService());
				if (i != action_list_length-1)
					sb.append(", ");				
				break;			
			case SET_TP_DST:
				sb.append("\"dstport\": ")
					.append(((OFActionTransportLayerDestination)action).getTransportPort());
				if (i != action_list_length-1)
					sb.append(", ");				
				break;			
			case SET_TP_SRC:
				sb.append("\"srcport\": ")
					.append(((OFActionTransportLayerSource)action).getTransportPort());
				if (i != action_list_length-1)
					sb.append(", ");		
				break;
			case SET_VLAN_ID:
			case SET_VLAN_PCP:
			case STRIP_VLAN:
			case VENDOR:
			case OPAQUE_ENQUEUE:
			}
		}
		sb.append("}]]\n");
		}
		
		
		return sb.toString();
	}
	
	/**
	 * Returns a JSON object of the provided OF Action
	 * @param action to be serialized to JSON
	 * @return
	 */
	public static JSONObject getAction(OFAction action) {
		JSONObject json = new JSONObject();
		switch (action.getType()) {
			case OUTPUT:
				json.put("outport", (0x0FFFF&(((OFActionOutput)action).getPort())));
				break;
			case SET_DL_DST:
				json.put("dstmac", ((OFActionDataLayerDestination)action).getDataLayerAddress());
				break;
			case SET_DL_SRC:
				json.put("srcmac", ((OFActionDataLayerSource)action).getDataLayerAddress());
				break;
			case SET_NW_DST:
				int ipDst = ((OFActionNetworkLayerDestination)action).getNetworkAddress();
				json.put("dstip", IPv4.toIPv4AddressBytes(ipDst));
				break;
			case SET_NW_SRC:
				int ipSrc = ((OFActionNetworkLayerSource)action).getNetworkAddress();
				json.put("dstmac", IPv4.toIPv4AddressBytes(ipSrc));
				break;
			case SET_NW_TOS:
				json.put("tos", ((OFActionNetworkTypeOfService)action).getNetworkTypeOfService());
			case SET_TP_DST:
				json.put("dstport", (0x0FFFF&((OFActionTransportLayer)action).getTransportPort()));
				break;
			case SET_TP_SRC:
				json.put("srcport", (0x0FFFF&((OFActionTransportLayer)action).getTransportPort()));
				break;
			case SET_VLAN_ID:
			case SET_VLAN_PCP:
			case STRIP_VLAN:
			case VENDOR:
			case OPAQUE_ENQUEUE:
		}
		return json;
	}
	
}
