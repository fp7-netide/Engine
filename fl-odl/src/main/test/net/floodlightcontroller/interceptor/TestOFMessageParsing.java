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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public class TestOFMessageParsing {

	private final String PACKET_IN = "[\"packet\", {\"raw\": [186, 100, 113, 119, 229, 86, 190, 234, 9, 83, 209, 248, 8, 0, 69, 0, 0, 84, 110, 53, 0, 0, 64, 1, 248, 113, 10, 0, 0, 2, 10, 0, 0, 1, 0, 0, 157, 245, 111, 210, 0, 4, 13, 76, 53, 84, 190, 144, 6, 0, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55], \"switch\": 1, \"inport\": 1}]";
	
	private final String SWITCH_JOIN_BEGIN = "[\"switch\", \"join\", 1, \"BEGIN\"]"; 
	private final String SWITCH_JOIN_END   = "[\"switch\", \"join\", 1, \"END\"]";
	
	private final String PORT_JOIN_BEGIN1 = "[\"port\", \"join\", 1, 1, true, true, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]";
	private final String PORT_JOIN_BEGIN2 = "[\"port\", \"join\", 1, 2, true, true, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]";
	
	private final String SWITCH_PART = "[\"switch\", \"part\", 1]";
	private final String PORT_PART = "[\"port\", \"part\", 1, 1]";
	
	@Test
	public void testSwitchBegin() {
		OFMessageSwitch msgSwitch = new OFMessageSwitch(SWITCH_JOIN_BEGIN);
		assertEquals("Switch ID not set correctly", 1L, msgSwitch.getId());
		assertEquals("Switch action not set correctly", "join", msgSwitch.getAction());
		assertTrue(msgSwitch.isBegin());
	}

	@Test
	public void testSwitchEnd() {
		OFMessageSwitch msgSwitch = new OFMessageSwitch(SWITCH_JOIN_END);
		assertEquals("Switch ID not set correctly", 1L, msgSwitch.getId());
		assertEquals("Switch action not set correctly", "join", msgSwitch.getAction());
		assertFalse(msgSwitch.isBegin());
	}
	
	@Test
	public void testSwitchPart() {
		OFMessageSwitch msgSwitch = new OFMessageSwitch(SWITCH_PART);
		assertEquals("Switch ID not set correctly", 1L, msgSwitch.getId());
		assertEquals("Switch action not set correctly", "part", msgSwitch.getAction());
		assertFalse(msgSwitch.isBegin());
	}
	
	@Test
	public void testPortJoin() {
		OFMessagePort msgPort = new OFMessagePort(PORT_JOIN_BEGIN1);
		assertEquals("Switch ID not set correctly", 1L, msgPort.getSwitchId());
		assertEquals("Port action not set correctly", "join", msgPort.getAction());
		assertEquals("Port number not set correctly", 1, msgPort.getPortNo());
		assertEquals("Port Feature 1 not set correctly", "OFPPF_COPPER", msgPort.getPortFeatures().get(0));
		assertEquals("Port Feature 1 not set correctly", "OFPPF_10GB_FD", msgPort.getPortFeatures().get(1));
		assertEquals("OFPort no not set correctly", 1, msgPort.getOfPort().getPortNumber());
		assertEquals("OFPort no not set correctly", 13, msgPort.getOfPort().getSupportedFeatures());
	}
	
	@Test
	public void testPortPart() {
		OFMessagePort msgPort = new OFMessagePort(PORT_PART);
		assertEquals("Switch ID not set correctly", 1L, msgPort.getSwitchId());
		assertEquals("Port action not set correctly", "part", msgPort.getAction());
		assertEquals("Port number not set correctly", 1, msgPort.getPortNo());
		assertEquals("Port Features is not empty", 0, msgPort.getPortFeatures().size());
		assertNull(msgPort.getOfPort());
	}
}
