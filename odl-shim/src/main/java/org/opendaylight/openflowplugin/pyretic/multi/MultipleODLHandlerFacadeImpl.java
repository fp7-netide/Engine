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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.openflowplugin.pyretic.*;
import org.opendaylight.openflowplugin.pyretic.Utils.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;


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
        private DataBroker dataBroker;

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

            simpleLearningSwitch.setBackendChannel(channel);
            simpleLearningSwitch.onSwitchAppeared(appearedTablePath);
            simpleLearningSwitch.setDataBroker(this.dataBroker);

            /**
            * We update mapping of already instantiated LearningSwitchHandlers
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

    @Override
    public void setBackendChannel(BackendChannel channel) {
        this.channel = channel;
    }

    @Override
    public void sendToSwitch(JSONArray json) {
        Map<InstanceIdentifier<Node>, PacketProcessingListener> ppl = this.packetInDispatcher.getHandlerMapping();
        boolean sent = false;

        Iterator iter = ppl.keySet().iterator();
        while (iter.hasNext() && !sent) {
            ODLHandlerSimpleImpl simpleLearning = (ODLHandlerSimpleImpl)ppl.get(iter.next());
            String nodeid = simpleLearning.getNodeId().getValue();
            Integer swtch = -1;
            try {
                JSONObject packet = (JSONObject)json.get(1);
                swtch = ((Long) packet.get("switch")).intValue();
            } catch(Exception e) {
                swtch = ((Long)json.get(1)).intValue();
            }
            finally {
                if (("openflow:"+swtch).equalsIgnoreCase(nodeid)) {
                    simpleLearning.sendToSwitch(json);
                    sent = true;
                    break;
                }
            }
        }
        if (!sent)
            System.out.println("Simple learning switch is null or does not exist");
    }

    public void setDataBroker(DataBroker data) {
        this.dataBroker = data;
    }


}
