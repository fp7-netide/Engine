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
 *     aleckey
 */
package net.floodlightcontroller.interceptor;

/**
 * Parses an OF Switch message into its relevant properties
 *
 */
public class OFMessageSwitch {
	private String action;
	private long id;
	private boolean begin = false;

	/**@return the action */
	public String getAction() {
		return action;
	}

	/** @return the id */
	public long getId() {
		return id;
	}

	/** @return the begin */
	public boolean isBegin() {
		return begin;
	}
		
	public OFMessageSwitch() { }
	
	/**
	 * Parses message into relevant properties
	 * @param rawMessage Assumes the following format: ["switch", "join", 1, "BEGIN"] and parses into properties
	 * @return
	 */
	public OFMessageSwitch(String rawMessage) {
		parseMessage(rawMessage);
	}
	
	/**
	 * Parses message into relevant properties
	 * @param rawMessage Assumes the following format: ["switch", "join", 1, "BEGIN"] and parses into properties
	 * @return
	 */
	public void parseMessage(String rawMessage) {
		String tmp = rawMessage.substring(1, rawMessage.length()-1); //STRIP [ ]
		String[] props = tmp.split(","); 

		//TODO: CONVERT TO REGEX!!!
		this.action = props[1].trim().substring(1, props[1].trim().length()-1);
		this.id = Long.parseLong(props[2].trim());
		if (props.length > 3) {
			String subAction = props[3].trim().substring(1, props[3].trim().length()-1);
			if ( (this.action.equals("join")) && (subAction.equals("BEGIN")) )
				this.begin = true;
		}
	}
	
}
