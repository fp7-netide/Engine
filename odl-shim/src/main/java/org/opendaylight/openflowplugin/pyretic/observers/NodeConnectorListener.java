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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
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
    private boolean flag=false;
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

    // Check https://github.com/opendaylight/openflowplugin/blob/master/applications/lldp-speaker/src/main/java/org/opendaylight/openflowplugin/applications/lldpspeaker/NodeConnectorInventoryEventTranslator.java
    // for the rest of the cases (data changed, connector removed, etc)
    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.trace("Node connectors in inventory changed: {} created, {} updated, {} removed",
                change.getCreatedData().size(), change.getUpdatedData().size(), change.getRemovedPaths().size());

        // Iterate over created node connectors
        Map<InstanceIdentifier<?>, DataObject> createdElements = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : createdElements.entrySet()) {
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId = entry.getKey().firstIdentifierOf(NodeConnector.class);
            NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(nodeConnectorInstanceId).getId();
            String nodeID = nodeConnectorId.getValue();
            FlowCapableNodeConnector flowCon = (FlowCapableNodeConnector) entry.getValue();
        	this.createTopology(nodeID, flowCon);
        }

        Map<InstanceIdentifier<?>, DataObject> updatedElements = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updatedElements.entrySet()) {
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId =
                    entry.getKey().firstIdentifierOf(NodeConnector.class);
            FlowCapableNodeConnector flowConnector = (FlowCapableNodeConnector) entry.getValue();
            if (isPortDown(flowConnector)) {
                //notifyNodeConnectorDisappeared(nodeConnectorInstanceId);
            } else {
                //notifyNodeConnectorAppeared(nodeConnectorInstanceId, flowConnector);
            }
        }

        for (InstanceIdentifier<?> removed : change.getRemovedPaths()) {
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId = removed.firstIdentifierOf(NodeConnector.class);
            NodeConnectorId nodeConnectorId = InstanceIdentifier.keyOf(nodeConnectorInstanceId).getId();
            String nodeID = nodeConnectorId.getValue();
            this.send_part(nodeID);
        }
    }


    private static boolean isPortDown(FlowCapableNodeConnector flowCapableNodeConnector) {
        PortState portState = flowCapableNodeConnector.getState();
        PortConfig portConfig = flowCapableNodeConnector.getConfiguration();
        return portState != null && portState.isLinkDown() ||
                portConfig != null && portConfig.isPORTDOWN();
    }


    /**
     *
     * @param nodeID
     * @param flowCon
     */
   private void createTopology (String nodeID, FlowCapableNodeConnector flowCon)
    {   
	    String[] nodeInfo = nodeID.split(":");
    	ArrayList<Integer> ports = new ArrayList<>();
    	int newPort=-10;
        int switchNum = Integer.parseInt(nodeInfo[1]);
        if (!nodeInfo[2].equals("LOCAL"))
        	newPort = Integer.parseInt(nodeInfo[2]);
        if (!port2switchMapping.containsKey(switchNum)) {
		    send_switch_join(switchNum);
		    if (newPort != -10) {
                ports.add(newPort);
                port2switchMapping.put(switchNum,ports);
                send_port_join(switchNum, flowCon);
            }
        }
        else {
        	if (newPort!=-10) {
        		ports = port2switchMapping.get(switchNum);
        		ports.add(newPort);
        		port2switchMapping.put(switchNum, ports);
			    send_port_join(switchNum, flowCon);
        	}
        }
    }

    private void send_port_join(int switchNum, FlowCapableNodeConnector flowCon)
    {  
        PortFeatures pf = flowCon.getCurrentFeature();
        ArrayList<String> features = new ArrayList<String>();
        if (pf.isCopper() == true)
           features.add("\"OFPPF_COPPER\"");
        if (pf.isTenGbFd() == true)
           features.add("\"OFPPF_10GB_FD\"");
        boolean CONF_UP = (flowCon.getConfiguration().isPORTDOWN());
        if (CONF_UP == false) CONF_UP = true;
        else CONF_UP = false;
            boolean STAT_UP = flowCon.getState().isLinkDown();
        if (STAT_UP == false) STAT_UP = true;
        else STAT_UP = false;
        String stringToChannel = "[\"port\", \"join\", " + switchNum + ", " + flowCon.getPortNumber().getUint32() + ", "+CONF_UP+", "+STAT_UP+", " + features.toString()+"]\n";
        sleep(1000);
        System.out.println(stringToChannel);
        this.channel.push(stringToChannel);        
    }

    private void send_switch_join(int switchNum) {
    	System.out.println("New switch with name " + "s"+switchNum);
        String stringToChannel = "[\"switch\", \"join\", " + switchNum + ", \"BEGIN\"]\n";
	    System.out.println(stringToChannel);
        this.channel.push(stringToChannel);
	    stringToChannel = "[\"switch\", \"join\", " + switchNum + ", \"END\"]\n";
	    System.out.println(stringToChannel);
        this.channel.push(stringToChannel);
    }

    /**
     * We receive a node (switch or port) in order to send the corresponding
     * switch part or port part to the backend
     * @param node
     */
    private void send_part(String node) {
        String[] nodeInfo = node.split(":");
        // nodeInfo[1] contains the switch id, while nodeInfo[2] contains the port id
        // if the node received is a port, then nodeInfo[2] has "LOCAL" as value.
        if (nodeInfo.length == 2 || nodeInfo[2].equalsIgnoreCase("LOCAL")) {
            System.out.println("Switch with name " + "s" + nodeInfo[1] + " disconnected");
            String stringToChannel = "[\"switch\", \"part\", " + nodeInfo[1] + "]";
            System.out.println(stringToChannel);
            this.channel.push(stringToChannel+"\n");
            // We remove the switch from our port2switchMapping
            if (port2switchMapping.containsKey(nodeInfo[1])) {
                port2switchMapping.remove(nodeInfo[1]);
            }
        }
        else {
            String stringToChannel = "[\"port\", \"part\", " + nodeInfo[1] + ", " + nodeInfo[2] +"]";
            System.out.println(stringToChannel);
            this.channel.push(stringToChannel+"\n");
            // If the switch info has not being removed yet, we clear the port in nodeInfo[2]
            if (port2switchMapping.containsKey(nodeInfo[1])) {
                ArrayList<Integer> arrayAux = port2switchMapping.get(nodeInfo[1]);
                for (int i = 0; i < arrayAux.size(); i++) {
                    if (arrayAux.get(i) == Integer.parseInt(nodeInfo[2])) {
                        arrayAux.remove(i);
                        break;
                    }
                }
                if (arrayAux.size() > 0) {
                    port2switchMapping.put(Integer.parseInt(nodeInfo[1]), arrayAux);
                }
            }
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
