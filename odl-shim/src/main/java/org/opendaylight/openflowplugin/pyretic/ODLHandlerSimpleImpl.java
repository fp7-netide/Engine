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

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.json.simple.JSONArray;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.openflowplugin.pyretic.Utils.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import com.telefonica.pyretic.backendchannel.BackendChannel;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;


/**
 * Simple Learning Switch implementation which does mac learning for one switch.
 *
 /**
 * Created by Jennifer Hernández Bécares
 */
public class ODLHandlerSimpleImpl implements ODLHandler, PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(ODLHandler.class);

    private static final byte[] ETH_TYPE_IPV4 = new byte[] { 0x08, 0x00 };
    private static final byte[] ETH_TYPE_IPV6 = new byte[] {(byte) 0x86, (byte) 0xdd};
    private static final byte[] ETH_TYPE_LLDP = new byte[] {(byte) 0x88, (byte) 0xcc};
    private static final byte[] ETH_TYPE_ARP  = new byte[] {(byte) 0x08, (byte) 0x06};


    private static final int DIRECT_FLOW_PRIORITY = 512;

    private DataChangeListenerRegistrationHolder registrationPublisher;
    private FlowCommitWrapper dataStoreAccessor;
    private PacketProcessingService packetProcessingService;

    private boolean iAmLearning = false;

    private NodeId nodeId;
    private AtomicLong flowIdInc = new AtomicLong();
    private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);
    
    private InstanceIdentifier<Node> nodePath;
    private InstanceIdentifier<Table> tablePath;

    private Map<MacAddress, NodeConnectorRef> mac2portMapping;
    private Set<String> coveredMacPaths;

    private BackendChannel channel;
    private DataBroker dataBroker;

    @Override
    public synchronized void onSwitchAppeared(InstanceIdentifier<Table> appearedTablePath) {

        if (iAmLearning) {
            LOG.debug("already learning a node, skipping {}", nodeId.getValue());
            return;
        }

        LOG.debug("expected table acquired, learning ..");

        // disable listening - simple learning handles only one node (switch)
        if (registrationPublisher != null) {
            try {
                LOG.debug("closing dataChangeListenerRegistration");
                registrationPublisher.getDataChangeListenerRegistration().close();
            } catch (Exception e) {
                LOG.error("closing registration upon flowCapable node update listener failed: " + e.getMessage(), e);
            }
        }

        iAmLearning = true;
        
        tablePath = appearedTablePath;
        nodePath = tablePath.firstIdentifierOf(Node.class);
        nodeId = nodePath.firstKeyOf(Node.class, NodeKey.class).getId();
        mac2portMapping = new HashMap<>();
        coveredMacPaths = new HashSet<>();

        // start forwarding all packages to controller
        FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
        FlowKey flowKey = new FlowKey(flowId);
        InstanceIdentifier<Flow> flowPath = InstanceIdentifierUtils.createFlowPath(tablePath, flowKey);

        int priority = 0;
        FlowBuilder allToCtrlFlow = FlowUtils.createFwdAllToControllerFlow(
                InstanceIdentifierUtils.getTableId(tablePath), priority, flowId);

        LOG.debug("writing packetForwardToController flow");
        dataStoreAccessor.writeFlowToConfig(flowPath, allToCtrlFlow.build());
    }

    // new
    private void sleep() {
        try {
            Thread.sleep(800);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    // end new


    @Override
    public void setRegistrationPublisher(DataChangeListenerRegistrationHolder registrationPublisher) {
        this.registrationPublisher = registrationPublisher;
    }

    @Override
    public void setDataStoreAccessor(FlowCommitWrapper dataStoreAccessor) {
        this.dataStoreAccessor = dataStoreAccessor;
    }

    @Override
    public void setPacketProcessingService(PacketProcessingService packetProcessingService) {
        this.packetProcessingService = packetProcessingService;
    }

    public synchronized void onPacketReceived(PacketReceived notification) {
        byte[] etherType = PacketUtils.extractEtherType(notification.getPayload());
        byte[] dstMacRaw = PacketUtils.extractDstMac(notification.getPayload());
        byte[] srcMacRaw = PacketUtils.extractSrcMac(notification.getPayload());


        MacAddress srcMac = PacketUtils.rawMacToMac(srcMacRaw);

        NodeConnectorKey ingressKey = InstanceIdentifierUtils.getNodeConnectorKey(notification.getIngress().getValue());
        String path  = ingressKey.getId().getValue();

        String [] msg = null;
        String switch_s = null;
        String inport = null;


        if(path.contains(":")) {
            msg = path.split(":");
            switch_s = msg[1];
            inport = msg[2];

            List<Integer> raw = new ArrayList<Integer>();
            for(byte b:notification.getPayload()){
                int aux = (int)b;
                if (aux < 0){
                    aux = 256 + aux;
                }
                raw.add(aux);
            }

            if (Arrays.equals(ETH_TYPE_IPV4, etherType)) {
                JSONObject json = new JSONObject();
                json.put("switch", Integer.parseInt(switch_s));
                json.put("inport", Integer.parseInt(inport));
                json.put("raw",raw);

                List<String> p = new ArrayList<String>();
                p.add("\"packet\"");
                p.add(json.toString());

                this.channel.push(p.toString() + "\n");
                mac2portMapping.put(srcMac, notification.getIngress());
            }
            else if (Arrays.equals(ETH_TYPE_IPV6, etherType)) {
		        System.out.println("IPV6 arrived - not handling ipv6");
                mac2portMapping.put(srcMac, notification.getIngress());
            }
            else if (Arrays.equals(ETH_TYPE_ARP, etherType)) {
                // Handle ARP packet
                JSONObject json = new JSONObject();

                json.put("switch", Integer.parseInt(switch_s));
                json.put("inport", Integer.parseInt(inport));
                json.put("raw",raw);

                List<String> p = new ArrayList<String>();
                p.add("\"packet\"");
                p.add(json.toString());

                this.channel.push(p.toString() + "\n");
                mac2portMapping.put(srcMac, notification.getIngress());

            }
            else if(Arrays.equals(ETH_TYPE_LLDP,etherType)){
                //Handle LLDP packet
                System.out.println("LLDP arrived");
                //List<String> p = new ArrayList<String>();
                //p.add("\"link\"");
                //p.add()

                mac2portMapping.put(srcMac, notification.getIngress());
            }
            else {
                LOG.debug("Unknown packet arrived.\nThis shouldn't be happening");
            }
        }
        else
        {
            throw new IllegalArgumentException("String " + path + " does not contain -");
        }
    }


    public void setBackendChannel(BackendChannel channel)
    {
        this.channel = channel;
    }

    static InstanceIdentifier<NodeConnector> createNodeConnectorId(String nodeKey, String nodeConnectorKey) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId(nodeKey)))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnectorKey)))
                .build();
    }
    //

    @Override
    public void sendToSwitch(JSONArray json) {
        String type = json.get(0).toString();

        if (type.equals("packet")) {
            JSONObject packet = (JSONObject)json.get(1);
            Integer swtch = ((Long) packet.get("switch")).intValue();
            Integer inport = ((Long) packet.get("inport")).intValue();
            Integer outport = ((Long) packet.get("outport")).intValue();

            String inNodeKey = "openflow:" + swtch.toString();
            String inPort = inport.toString();
            String outPort = outport.toString();

            // Get the raw packet from the json
            List<Long> raw = (List<Long>) packet.get("raw");
            StringBuilder sb = new StringBuilder("");
            for (Long b : raw) {
                sb.append(OutputUtils.fromDecimalToHex(b));
            }
            byte[] payload = OutputUtils.toByteArray(sb.toString());

            /*
                Ethernet types in pyretic:
                             HEX    -> Decimal
                LLDP_TYPE  = 0x88cc -> 35020
                ARP_TYPE   = 0x806  -> 2054
                IP_TYPE    = 0x800  -> 2048
                IPV6_TYPE  = 0x86dd -> 34525
            */
            TransmitPacketInput input = null;

            input = OutputUtils.createPacketOut(inNodeKey, payload,
                        outPort, inPort);

            packetProcessingService.transmitPacket(input);
            LOG.debug("Packet transmitted");
        }

        else if (type.equals("install")) {
            System.out.println("install packet----------");
            System.out.println(json.toString());
            JSONObject match = (JSONObject) json.get(1);
            int priority = Integer.parseInt(json.get(2).toString());
            JSONArray actions = (JSONArray)json.get(3);
            for (int i = 0; i < actions.size(); i++) {
                FlowBuilder fb = FlowModUtils.createFlowBuilder(match, priority, (JSONObject)actions.get(i));
                if (!(fb == null)) {
                    final Flow flow = fb.build();
                    ReadWriteTransaction transaction = this.dataBroker.newReadWriteTransaction();
                    transaction.put(LogicalDatastoreType.CONFIGURATION,
                            InstanceIdentifierUtils.createFlowPath(this.tablePath, new FlowKey(flow.getId())),
                            flow, true);
                    transaction.submit();
                    System.out.println("Flow sent to switch");
                }
                else
                    LOG.debug("Ignoring install");
            }
        }

        else if (type.equals("inject_discovery_packet")) {

            Integer inNodeKey = ((Long)json.get(1)).intValue();
            Integer outport = ((Long)json.get(2)).intValue();
            String dpid = inNodeKey.toString();
            String port = outport.toString();
            MacAddress srcMac = new MacAddress("ff:ff:ff:ff:ff:ff");

            for (Map.Entry<MacAddress, NodeConnectorRef> entry : mac2portMapping.entrySet()) {

                NodeConnectorKey nodeConnectorKey = getNodeConnectorKey(entry.getValue());
                if (nodeConnectorKey.getId().getValue().equalsIgnoreCase(this.nodeId.getValue() + ":" + port)) {
                    srcMac = entry.getKey();
                }
            }

            if (!srcMac.getValue().equalsIgnoreCase("ff:ff:ff:ff:ff:ff")) {
                TransmitPacketInput input = null;

                input = LLDPDiscoveryUtils.createLLDPOut(dpid, port, srcMac);
                packetProcessingService.transmitPacket(input);
                System.out.println("LLDP (inject discovery packet) transmitted");
            }
            else
                System.out.println("Not transmitting");
        }

        else {
            LOG.debug("Different type <<<< " + type);
        }
    }

    public void setDataBroker(DataBroker data) {
        this.dataBroker = data;
    }

    /**
     * Used for comparing the name of the node with the one in MultipleODLHandlerFacadeImpl->SendToSwitch
     * @return
     */
    public NodeId getNodeId() {
        return nodeId;
    }

    // Auxiliary methods
    /**
     *
     * @param ref
     * @return
     */
    public static NodeKey getNodeKey(final NodeConnectorRef ref) {
        return ref.getValue().firstKeyOf(Node.class, NodeKey.class);
    }

    public static NodeId toTopologyNodeId(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nodeId) {
        return new NodeId(nodeId);
    }

    private static NodeId toTopologyNodeId(final NodeConnectorRef source) {
        return toTopologyNodeId(getNodeKey(source).getId());
    }

    public static NodeConnectorKey getNodeConnectorKey(final NodeConnectorRef ref) {
        return ref.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class);
    }

}