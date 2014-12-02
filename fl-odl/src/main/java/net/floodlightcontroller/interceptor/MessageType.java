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

/**
 * Describe your class here...
 *
 * @author aleckey
 *
 */
public enum MessageType {

     SWITCH("switch"),
     PORT("port"),
     PACKET("packet"),
     LINK("link"),
     FLOW_STATS_REPLY("flow_stats_reply"),
     UNSUPPORTED("unsupported");
     
 private final String value;

     MessageType(String v) {
     value = v;
 }

 public String value() {
     return value;
 }

 public static MessageType fromValue(String v) {
     for (MessageType c: MessageType.values()) {
         if (c.value.equals(v)) {
             return c;
         }
     }
     throw new IllegalArgumentException(v);
 }

 @Override
 public String toString() {
 	return this.value();
 }
}
