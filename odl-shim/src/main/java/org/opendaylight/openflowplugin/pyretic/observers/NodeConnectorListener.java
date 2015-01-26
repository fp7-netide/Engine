/*
 * Copyright (c) 2014 Pacnet and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.pyretic.observers;
import java.util.ArrayList;
import java.util.Map;

import com.telefonica.pyretic.backendchannel.BackendChannel;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * NodeConnectorInventoryEventTranslator is listening for changes in inventory operational DOM tree
 * and update LLDPSpeaker and topology.
 */
public class NodeConnectorListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NodeConnectorListener.class);

    private final ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    private BackendChannel channel;
    private HashMap<Integer, ArrayList<Integer>> port2switchMapping;

    public NodeConnectorListener(DataBroker dataBroker) {
        dataChangeListenerRegistration = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class)
                        .child(NodeConnector.class)
                        .augmentation(FlowCapableNodeConnector.class)
                        .build(),
                this, AsyncDataBroker.DataChangeScope.BASE);
        port2switchMapping = new HashMap<>();
    }

    @Override
    public void close() {
        dataChangeListenerRegistration.close();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        sleep(2000);
        LOG.trace("Node connectors in inventory changed: {} created, {} updated, {} removed",
                change.getCreatedData().size(), change.getUpdatedData().size(), change.getRemovedPaths().size());

        // Iterate over created node connectors
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            System.out.println("Node created");

            //System.out.println("Entry key: " + entry.getKey().toString());
            //System.out.println("Entry value:" + entry.getValue().toString());

            HashMap<String, String> valueMap = this.getValueMap(entry.getValue().toString());

            String elementName = valueMap.get("getName");
            System.out.println("Name: " + elementName);
            this.createTopology(elementName);

            FlowCapableNodeConnector flowConnector = (FlowCapableNodeConnector) entry.getValue();
            if (!isPortDown(flowConnector)) {
                //notifyNodeConnectorAppeared(nodeConnectorInstanceId, flowConnector);
            }
        }

       /* // Iterate over updated node connectors (port down state may change)
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            System.out.println("Node updated");
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId =
                    entry.getKey().firstIdentifierOf(NodeConnector.class);
            FlowCapableNodeConnector flowConnector = (FlowCapableNodeConnector) entry.getValue();
            if (isPortDown(flowConnector)) {
                //notifyNodeConnectorDisappeared(nodeConnectorInstanceId);
            } else {
                //notifyNodeConnectorAppeared(nodeConnectorInstanceId, flowConnector);
            }
        }

        // Iterate over removed node connectors
        for (InstanceIdentifier<?> removed : change.getRemovedPaths()) {
            System.out.println("Node removed");
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId = removed.firstIdentifierOf(NodeConnector.class);
            //notifyNodeConnectorDisappeared(nodeConnectorInstanceId);
        }*/
    }

    private static boolean isPortDown(FlowCapableNodeConnector flowCapableNodeConnector) {
        PortState portState = flowCapableNodeConnector.getState();
        PortConfig portConfig = flowCapableNodeConnector.getConfiguration();
        return portState != null && portState.isLinkDown() ||
                portConfig != null && portConfig.isPORTDOWN();
    }

    private HashMap<String, String> getValueMap(String entryValue)
    {
        HashMap<String, String> map = new HashMap<String,String>();
        String delims = "[ {}\\[\\],]";
        String [] temp = entryValue.split(delims);
        for (int i = 0; i < temp.length && temp[i] != ""; i++)
        {
            String delimEqual = "=";
            if (temp[i].contains(delimEqual))
            {
                String [] tempEq = temp[i].split("["+delimEqual+"]");

                if (tempEq.length == 2 && tempEq[0] != "" && tempEq[1] != ""){
                    map.put(tempEq[0], tempEq[1]);
                }

            }
        }
        return map;
    }

    /**
     * Makes switch and port joins from the information received from onDataChanged
     * @param value
     */
    private void createTopology (String value)
    {
        String del1 = "-";
        // Interface appeared
        if (value.contains(del1))
        {
            ArrayList<Integer> ports = new ArrayList<>();

            int switchNum = Integer.parseInt(Character.toString(value.charAt(1)));
            int newPort = Integer.parseInt(Character.toString(value.charAt(6)));

            if (this.port2switchMapping.size() > 0 && this.port2switchMapping.containsKey(switchNum)) {
                ports = this.port2switchMapping.get(switchNum);
                ports.add(newPort);
            }
            else {
                ports.add(newPort);
            }
            this.port2switchMapping.put(switchNum, ports);

            // Switch join begins
            System.out.println("New interface with name " + value);
            String stringToChannel = "[\"switch\", \"join\", " + switchNum + ", \"BEGIN\"]";
            System.out.println(stringToChannel);
            this.channel.push(stringToChannel);
            sleep(2000);

            // All existing ports join
            for (int i = 0; i < ports.size(); i++) {
                stringToChannel = "[\"port\", \"join\", " + switchNum + ", " + ports.get(i) + ", true, false, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]";
                System.out.println(stringToChannel);
                this.channel.push(stringToChannel);
                sleep(2000);
            }

            // Switch join ends
            stringToChannel = "[\"switch\", \"join\", " + switchNum + ", \"END\"]";
            System.out.println(stringToChannel);
            this.channel.push(stringToChannel);
            sleep(2000);
        }
        else // Switch appeared
        {
            //System.out.println("New switch with name " + value);
        }
    }

    public void setBackendChannel(BackendChannel channel) {
        this.channel = channel;
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
