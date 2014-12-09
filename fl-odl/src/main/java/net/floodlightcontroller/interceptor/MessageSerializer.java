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

import org.json.JSONObject;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.U8;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class MessageSerializer {

	public MessageSerializer() { }
	
	public static String serializeMessage(OFPacketOut packetOut) {
		//["packet", {"outport": 1, "protocol": 2, "header_len": 14, "inport": 2, 
		// "dstip": [49, 48, 46, 48, 46, 48, 46, 49], 
		// "srcmac": [99, 101, 58, 97, 56, 58, 100, 100, 58, 99, 102, 58, 49, 99, 58, 97, 101], "dstmac": [99, 101, 58, 97, 54, 58, 99, 51, 58, 100, 100, 58, 56, 57, 58, 99, 51], 
		// "raw": [206, 166, 195, 221, 137, 195, 206, 168, 221, 207, 28, 174, 8, 6, 0, 1, 8, 0, 6, 4, 0, 2, 206, 168, 221, 207, 28, 174, 10, 0, 0, 2, 206, 166, 195, 221, 137, 195, 10, 0, 0, 1], 
		// "payload_len": 42, "switch": 1, "ethtype": 2054, "srcip": [49, 48, 46, 48, 46, 48, 46, 50] }] + TERM_CHAR

		JSONObject json = new JSONObject();
		json.put("buf", packetOut.getBufferId());
		json.put("inport", packetOut.getInPort());
		json.put("raw", packetOut.getPacketData());
		json.put("header_len", packetOut.getActionsLength());
		String[] actionArray = new String[packetOut.getActions().size()];
		for (int i=0; i<packetOut.getActions().size(); i++) {
			actionArray[i] = packetOut.getActions().get(i).getType().toString();
		}
		json.put("actions", actionArray);
		
		StringBuilder sb = new StringBuilder("[\"packet\", ");
		sb.append(json.toString());
		sb.append("]\n");
		return sb.toString();
	}

	public static String serializeMessage(OFFlowMod flowMod) {
		//["install", 0, [{"outport": 2}], 
		//  {"dstip": [49, 48, 46, 48, 46, 48, 46, 49], "srcip": [49, 48, 46, 48, 46, 48, 46, 50],
		//   "dstmac": [54, 97, 58, 50, 100, 58, 55, 102, 58, 50, 57, 58, 99, 56, 58, 54, 49], "srcmac": [49, 50, 58, 57, 51, 58, 50, 99, 58, 52, 97, 58, 52, 56, 58, 50, 52],
		//   "dstport": 0, "srcport": 0
		//   "protocol": 1,  "tos": 0, "inport": 1, "switch": 2, "ethtype": 2048} ]	
		
		JSONObject json = new JSONObject();
		json.put("protocol", 1);
		for (OFAction action: flowMod.getActions()) {
			switch (action.getType()) {
				case SET_DL_DST:
					json.put("dstmac", flowMod.getMatch().getDataLayerDestination());
					break;
				case SET_DL_SRC:
					json.put("srcmac", flowMod.getMatch().getDataLayerSource());
					break;
				case SET_NW_DST:
					json.put("dstip", cidrToString(flowMod.getMatch().getNetworkDestination(), flowMod.getMatch().getNetworkDestination()));
					break;
				case SET_NW_SRC:
					json.put("srcip", cidrToString(flowMod.getMatch().getNetworkSource(), flowMod.getMatch().getNetworkDestination()));
					break;
				case SET_TP_DST:
					json.put("dstport", flowMod.getMatch().getTransportDestination());
					break;
				case SET_TP_SRC:
					json.put("srcport", flowMod.getMatch().getTransportSource());
					break;
				case SET_NW_TOS:
					json.put("tos", flowMod.getMatch().getNetworkTypeOfService());
					break;
				default:
			}
		}
		json.put("inport", flowMod.getMatch().getInputPort());
		//json.put("protocol", flowMod.getMatch().getNetworkProtocol());
		json.put("ethtype", flowMod.getMatch().getDataLayerType());	
		
		StringBuilder sb = new StringBuilder("[\"");
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
		sb.append("\", ").append(flowMod.getIdleTimeout())
					     .append(", [{\"outport\": ")
					     .append(flowMod.getOutPort())
					     .append("}], ");
		sb.append(json.toString());
		sb.append("]\n");
		return sb.toString();
	}
	
	private static String cidrToString(int ip, int prefix) {
        String str;
        if (prefix >= 32) {
            str = ipToString(ip);
        } else {
            // use the negation of mask to fake endian magic
            int mask = ~((1 << (32 - prefix)) - 1);
            str = ipToString(ip & mask) + "/" + prefix;
        }

        return str;
	}
	protected static String ipToString(int ip) {
        return Integer.toString(U8.f((byte) ((ip & 0xff000000) >> 24))) + "."
                + Integer.toString((ip & 0x00ff0000) >> 16) + "."
                + Integer.toString((ip & 0x0000ff00) >> 8) + "."
                + Integer.toString(ip & 0x000000ff);
    }
	
}
