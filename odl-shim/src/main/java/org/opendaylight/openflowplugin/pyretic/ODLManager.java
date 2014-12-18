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
 *     Telefonica I+D
 */
/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.pyretic;

import com.telefonica.pyretic.backendchannel.BackendChannel;
import org.json.simple.JSONObject;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

/**
 * 
 */
public interface ODLManager {

    /**
     * stop manager
     */
    void stop();

    /**
     * start manager
     */
    void start();

    /**
     * Set's Data Broker dependency.
     *
     * Data Broker is used to access overal operational and configuration
     * tree.
     *
     *  In simple Learning Switch handler, data broker is used to listen
     *  for changes in Openflow tables and to configure flows which will
     *  be provisioned down to the Openflow switch.
     *
     * inject {@link org.opendaylight.controller.sal.binding.api.data.DataBrokerService}
     * @param data
     */
    void setDataBroker(DataBroker data);

    /**
     * Set's Packet Processing dependency.
     *
     * Packet Processing service is used to send packet Out on Openflow
     * switch.
     *
     * inject {@link org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService}
     *
     * @param packetProcessingService
     */
    void setPacketProcessingService(
            PacketProcessingService packetProcessingService);

    PacketProcessingService getPacketProcessingService();
    /**
     * Set's Notification service dependency.
     *
     * Notification service is used to register for listening
     * packet-in notifications.
     *
     * inject {@link org.opendaylight.controller.sal.binding.api.NotificationService}
     * @param notificationService
     */
    void setNotificationService(NotificationService notificationService);

    void setBackendChannel(BackendChannel backend);

}
