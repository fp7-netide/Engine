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
package org.opendaylight.testshim;

import java.net.InetAddress;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class TestClient {
	private final static String TERM_CHAR = "\n";
	private final static String PACKET_IN = "[\"packet\", {\"raw\": [186, 100, 113, 119, 229, 86, 190, 234, 9, 83, 209, 248, 8, 0, 69, 0, 0, 84, 110, 53, 0, 0, 64, 1, 248, 113, 10, 0, 0, 2, 10, 0, 0, 1, 0, 0, 157, 245, 111, 210, 0, 4, 13, 76, 53, 84, 190, 144, 6, 0, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55], \"switch\": 1, \"inport\": 1}]\n";
	
	private final static String SWITCH_JOIN = "[\"switch\", \"join\", 1, \"BEGIN\"]\n" + 
											  "[\"port\", \"join\", 1, 3, true, true, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]\n" +
											  "[\"port\", \"join\", 1, 2, true, true, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]\n" +
											  "[\"port\", \"join\", 1, 1, true, true, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]\n" +
											  "[\"switch\", \"join\", 1, \"END\"]\n";
	
	private final static String SWITCH_PART = "[\"switch\", \"part\", 1]\n";
	
	public static void main(String[] args) {
		try {
			//NioClient client = new NioClient(InetAddress.getByName("www.google.com"), 80);
			NioClient client = new NioClient(InetAddress.getByName("localhost"), 41414);
			Thread t = new Thread(client);
			t.setDaemon(true);
			t.start();
			RspHandler handler = new RspHandler();
			
			client.send(SWITCH_JOIN.getBytes(), handler);
			//client.send(SWITCH_PART.getBytes(), handler);
			//client.send(PACKET_IN.getBytes(), handler);
			handler.waitForResponse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
