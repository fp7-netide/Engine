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

package org.opendaylight.openflowplugin.pyretic.multi;

import com.telefonica.pyretic.backendchannel.BackendChannel;
import org.opendaylight.openflowplugin.pyretic.FlowCommitWrapper;
import org.opendaylight.openflowplugin.pyretic.ODLHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.openflowplugin.pyretic.DataChangeListenerRegistrationHolder;
import org.opendaylight.openflowplugin.pyretic.InstanceIdentifierUtils;
import org.opendaylight.openflowplugin.pyretic.ODLHandlerSimpleImpl;


/**
 * Listens to packetIn notification and
 * <ul>
 * <li>in HUB mode simply floods all switch ports (except ingress port)</li>
 * <li>in LSWITCH mode collects source MAC address of packetIn and bind it with ingress port.
 * If target MAC address is already bound then a flow is created (for direct communication between
 * corresponding MACs)</li>
 * </ul>
 */
public class MultipleODLHandlerFacadeImpl implements ODLHandler {


        private BackendChannel channel;

        private static final Logger LOG = LoggerFactory
                .getLogger(MultipleODLHandlerFacadeImpl.class);
        private FlowCommitWrapper dataStoreAccessor;
        private PacketProcessingService packetProcessingService;
        private PacketInDispatcherImpl packetInDispatcher;

        @Override
        public synchronized void onSwitchAppeared(InstanceIdentifier<Table> appearedTablePath) {
            LOG.debug("expected table acquired, learning ..");
        /**
         * appearedTablePath is in form of /nodes/node/node-id/table/table-id
         * so we shorten it to /nodes/node/node-id to get identifier of switch.
         *
         */
        InstanceIdentifier<Node> nodePath = InstanceIdentifierUtils.getNodePath(appearedTablePath);
        /**
        * We check if we already initialized dispatcher for that node,
        * if not we create new handler for switch.
        *
        */
        if (!packetInDispatcher.getHandlerMapping().containsKey(nodePath)) {
            // delegate this node (owning appearedTable) to SimpleLearningSwitchHandler
            ODLHandlerSimpleImpl simpleLearningSwitch = new ODLHandlerSimpleImpl();
            /**
            * We set runtime dependencies
            */
            simpleLearningSwitch.setDataStoreAccessor(dataStoreAccessor);
            simpleLearningSwitch.setPacketProcessingService(packetProcessingService);
            /**
            * We propagate table event to newly instantiated instance of learning switch
            */

            simpleLearningSwitch.setBackendChannel(channel);// new
            simpleLearningSwitch.onSwitchAppeared(appearedTablePath);


            /**
            * We update mapping of already instantiated LearningSwitchHanlders
            */
            packetInDispatcher.getHandlerMapping().put(nodePath, simpleLearningSwitch);
        }
    }

    @Override
    public void setRegistrationPublisher(
            DataChangeListenerRegistrationHolder registrationPublisher) {
//NOOP
    }

    @Override
    public void setDataStoreAccessor(FlowCommitWrapper dataStoreAccessor) {
        this.dataStoreAccessor = dataStoreAccessor;
    }

    @Override
    public void setPacketProcessingService(
            PacketProcessingService packetProcessingService) {
        this.packetProcessingService = packetProcessingService;
    }

    /**
     * @param packetInDispatcher
     */
    public void setPacketInDispatcher(PacketInDispatcherImpl packetInDispatcher) {
        this.packetInDispatcher = packetInDispatcher;
    }

    public void setBackendChannel(BackendChannel channel) {
        this.channel = channel;
    }
}
