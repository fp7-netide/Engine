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
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowplugin.pyretic;

import com.telefonica.pyretic.backendchannel.BackendChannel;
import org.json.simple.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Listens to packetIn notification and 
 * <ul>
 * <li>in HUB mode simply floods all switch ports (except ingress port)</li>
 * <li>in LSWITCH mode collects source MAC address of packetIn and bind it with ingress port. 
 * If target MAC address is already bound then a flow is created (for direct communication between 
 * corresponding MACs)</li>
 * </ul>
 */
public class ODLManagerSimpleImpl implements DataChangeListenerRegistrationHolder,
        ODLManager {
    
    protected static final Logger LOG = LoggerFactory
            .getLogger(ODLManagerSimpleImpl.class);

    private NotificationService notificationService;
    private PacketProcessingService packetProcessingService;
    private DataBroker data;

    private Registration packetInRegistration;

    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

    private BackendChannel channel;

    /**
     * @param notificationService the notificationService to set
     */
    @Override
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * @param packetProcessingService the packetProcessingService to set
     */
    @Override
    public void setPacketProcessingService(
            PacketProcessingService packetProcessingService) {
        this.packetProcessingService = packetProcessingService;
    }

    @Override
    public PacketProcessingService getPacketProcessingService() {
        return this.packetProcessingService;
    }

    /**
     * @param data the data to set
     */
    @Override
    public void setDataBroker(DataBroker data) {
        this.data = data;
    }

    /**
     * starting learning switch
     */
    @Override
    public void start() {
        LOG.debug("start() -->");

        FlowCommitWrapper dataStoreAccessor = new FlowCommitWrapperImpl(data);

        ODLHandlerSimpleImpl odlHandler = new ODLHandlerSimpleImpl();
        odlHandler.setRegistrationPublisher(this);
        odlHandler.setDataStoreAccessor(dataStoreAccessor);
        odlHandler.setPacketProcessingService(packetProcessingService);
        odlHandler.setBackendChannel(channel);
        odlHandler.setDataBroker(this.data);
        packetInRegistration = notificationService.registerNotificationListener(odlHandler);

        channel.setHandler(odlHandler); // new

        WakeupOnNode wakeupListener = new WakeupOnNode();
        wakeupListener.setODLHandler(odlHandler);
        dataChangeListenerRegistration = data.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class)
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class).build(),
                wakeupListener,
                DataBroker.DataChangeScope.SUBTREE);
        LOG.debug("start() <--");
    }
    
    /**
     * stopping learning switch 
     */
    @Override
    public void stop() {
        LOG.debug("stop() -->");
        //TODO: remove flow (created in #start())
        try {
            packetInRegistration.close();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        try {
            dataChangeListenerRegistration.close();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.debug("stop() <--");
    }
    
   
    @Override
    public ListenerRegistration<DataChangeListener> getDataChangeListenerRegistration() {
        return dataChangeListenerRegistration;
    }

    @Override
    public void setBackendChannel(BackendChannel channel) {
        this.channel = channel;
    }

}