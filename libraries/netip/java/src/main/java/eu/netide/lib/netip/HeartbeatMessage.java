/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.netide.lib.netip;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class HeartbeatMessage extends Message {
    public HeartbeatMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.HEARTBEAT);
    }
}
