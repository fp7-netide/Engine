/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.pyretic;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import com.telefonica.pyretic.backendchannel.BackendChannel;



/**
 * Simple Learning Switch implementation which does mac learning for one switch.
 * 
 * 
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


    @Override
    public synchronized void onSwitchAppeared(InstanceIdentifier<Table> appearedTablePath) {

        System.out.println("------> On switch appeared... LearningSwitchHandlerSimpleImp");

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
        // create flow in table with id = 0, priority = 4 (other params are
        // defaulted in OFDataStoreUtil)
        FlowBuilder allToCtrlFlow = FlowUtils.createFwdAllToControllerFlow(
                InstanceIdentifierUtils.getTableId(tablePath), priority, flowId);

        System.out.println("--->writing packetForwardToController flow");
        LOG.debug("writing packetForwardToController flow");
        dataStoreAccessor.writeFlowToConfig(flowPath, allToCtrlFlow.build());
    }

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

    @Override
 /*   public void onPacketReceived(PacketReceived notification) {

        System.out.println("---> On packet received in LSHandler Simple impl");
        if (!iAmLearning) {
            // ignoring packets - this should not happen
            return;
        }

        LOG.debug("Received packet via match: {}", notification.getMatch());

        // detect and compare node - we support one switch
        if (!nodePath.contains(notification.getIngress().getValue())) {
            return;
        }

        // read src MAC and dst MAC
        byte[] dstMacRaw = PacketUtils.extractDstMac(notification.getPayload());
        byte[] srcMacRaw = PacketUtils.extractSrcMac(notification.getPayload());
        byte[] etherType = PacketUtils.extractEtherType(notification.getPayload());

        MacAddress dstMac = PacketUtils.rawMacToMac(dstMacRaw);
        MacAddress srcMac = PacketUtils.rawMacToMac(srcMacRaw);

        NodeConnectorKey ingressKey = InstanceIdentifierUtils.getNodeConnectorKey(notification.getIngress().getValue());

        LOG.debug("Received packet from MAC match: {}, ingress: {}", srcMac, ingressKey.getId());
        LOG.debug("Received packet to   MAC match: {}", dstMac);
        LOG.debug("Ethertype: {}", Integer.toHexString(0x0000ffff & ByteBuffer.wrap(etherType).getShort()));

        // learn by IPv4 traffic only
        if (Arrays.equals(ETH_TYPE_IPV4, etherType)) {
            NodeConnectorRef previousPort = mac2portMapping.put(srcMac, notification.getIngress());
            if (previousPort != null && !notification.getIngress().equals(previousPort)) {
                NodeConnectorKey previousPortKey = InstanceIdentifierUtils.getNodeConnectorKey(previousPort.getValue());
                LOG.debug("mac2port mapping changed by mac {}: {} -> {}", srcMac, previousPortKey, ingressKey.getId());
            }
            // if dst MAC mapped:
            NodeConnectorRef destNodeConnector = mac2portMapping.get(dstMac);
            if (destNodeConnector != null) {
                synchronized (coveredMacPaths) {
                    if (!destNodeConnector.equals(notification.getIngress())) {
                        // add flow
                        addBridgeFlow(srcMac, dstMac, destNodeConnector);
                        addBridgeFlow(dstMac, srcMac, notification.getIngress());
                    } else {
                        LOG.debug("useless rule ignoring - both MACs are behind the same port");
                    }
                }
                LOG.debug("packetIn-directing.. to {}",
                        InstanceIdentifierUtils.getNodeConnectorKey(destNodeConnector.getValue()).getId());
                sendPacketOut(notification.getPayload(), notification.getIngress(), destNodeConnector);
            } else {
                // flood
                LOG.debug("packetIn-still flooding.. ");
                flood(notification.getPayload(), notification.getIngress());
            }
        } else {
            // non IPv4 package
            flood(notification.getPayload(), notification.getIngress());
        }

    }*/
    public void onPacketReceived(PacketReceived notification) {
        System.out.println("--------------New packet arrived");

        byte[] etherType = PacketUtils.extractEtherType(notification.getPayload());
        byte[] dstMacRaw = PacketUtils.extractDstMac(notification.getPayload());
        byte[] srcMacRaw = PacketUtils.extractSrcMac(notification.getPayload());

        MacAddress dstMac = PacketUtils.rawMacToMac(dstMacRaw);
        MacAddress srcMac = PacketUtils.rawMacToMac(srcMacRaw);

        NodeConnectorKey ingressKey = InstanceIdentifierUtils.getNodeConnectorKey(notification.getIngress().getValue());
        String path  = ingressKey.getId().getValue();

        //System.out.println("Received packet from MAC match: " + srcMac + " ingress: {}" +ingressKey.getId());
        //System.out.println("Received packet to   MAC match: " + dstMac);

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

            System.out.print("Ethertype: " ); // + etherType.toString());
            for(int i = 0; i < etherType.length; i++) {
                System.out.printf("%02x ",0xff & etherType[i]);
            }

            if (Arrays.equals(ETH_TYPE_IPV4, etherType)) {
                LOG.debug("IPV4 packet arrived");

                JSONObject json = new JSONObject();
                json.put("switch", Integer.parseInt(switch_s));
                json.put("inport", Integer.parseInt(inport));
                json.put("raw",raw);

                List<String> p = new ArrayList<String>();
                p.add("\"packet\"");
                p.add(json.toString());
                System.out.println(p);

                this.channel.push(p.toString() + "\n");

                mac2portMapping.put(srcMac, notification.getIngress());
            }
            else if (Arrays.equals(ETH_TYPE_IPV6, etherType)) {
                // Handle IPV6 packet
                /*LOG.debug("IPV6 packet arrived");

                JSONObject json = new JSONObject();

                json.put("switch", Integer.parseInt(switch_s));
                json.put("inport", Integer.parseInt(inport));
                json.put("raw",raw);

                List<String> p = new ArrayList<String>();
                p.add("\"packet\"");
                p.add(json.toString());
                System.out.println(p);
                this.channel.push(p.toString() + "\n");

                mac2portMapping.put(srcMac, notification.getIngress());
                System.out.println("--> json" + json.toJSONString());*/
            }
            else if (Arrays.equals(ETH_TYPE_ARP, etherType)) {
                // Handle ARP packet
                LOG.debug("ARP packet arrived");
                JSONObject json = new JSONObject();

                json.put("switch", Integer.parseInt(switch_s));
                json.put("inport", Integer.parseInt(inport));
                json.put("raw",raw);

                List<String> p = new ArrayList<String>();
                p.add("\"packet\"");
                p.add(json.toString());
                System.out.println(p);

                if (this.channel == null) System.out.println("Impossible to push");
                this.channel.push(p.toString() + "\n");

                mac2portMapping.put(srcMac, notification.getIngress());
                System.out.println("--> json" + json.toJSONString());

            }
            else if(Arrays.equals(ETH_TYPE_LLDP,etherType)){
                //Handle lldp packet
                LOG.debug("LLDP packet arrived");

                JSONObject json = new JSONObject();

                json.put("switch", Integer.parseInt(switch_s));
                json.put("inport", Integer.parseInt(inport));
                json.put("raw",raw);

                List<String> p = new ArrayList<String>();
                p.add("\"packet\"");
                p.add(json.toString());
                System.out.println(p);
                this.channel.push(p.toString() + "\n");

                mac2portMapping.put(srcMac, notification.getIngress());
                System.out.println("--> json" + json.toJSONString());
            }
            else {
                LOG.debug("Unknown packet arrived.\nThis shouldn't be happening");
            }
        }
        else
        {
            throw new IllegalArgumentException("String " + path+ " does not contain -");
        }
    }


    /**


    private void addBridgeFlow(MacAddress srcMac, MacAddress dstMac, NodeConnectorRef destNodeConnector) {
        synchronized (coveredMacPaths) {
            String macPath = srcMac.toString() + dstMac.toString();
            if (!coveredMacPaths.contains(macPath)) {
                LOG.debug("covering mac path: {} by [{}]", macPath,
                        destNodeConnector.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId());

                coveredMacPaths.add(macPath);
                FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
                FlowKey flowKey = new FlowKey(flowId);

                InstanceIdentifier<Flow> flowPath = InstanceIdentifierUtils.createFlowPath(tablePath, flowKey);

                Short tableId = InstanceIdentifierUtils.getTableId(tablePath);
                FlowBuilder srcToDstFlow = FlowUtils.createDirectMacToMacFlow(tableId, DIRECT_FLOW_PRIORITY, srcMac,
                        dstMac, destNodeConnector);
                srcToDstFlow.setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())));

                dataStoreAccessor.writeFlowToConfig(flowPath, srcToDstFlow.build());
            }
        }
    }

    private void flood(byte[] payload, NodeConnectorRef ingress) {
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(nodeConnectorId("0xfffffffb"));
        InstanceIdentifier<?> nodeConnectorPath = InstanceIdentifierUtils.createNodeConnectorPath(nodePath, nodeConnectorKey);
        NodeConnectorRef egressConnectorRef = new NodeConnectorRef(nodeConnectorPath);

        sendPacketOut(payload, ingress, egressConnectorRef);
    }

    private NodeConnectorId nodeConnectorId(String connectorId) {
        NodeKey nodeKey = nodePath.firstKeyOf(Node.class, NodeKey.class);
        StringBuilder stringId = new StringBuilder(nodeKey.getId().getValue()).append(":").append(connectorId);
        return new NodeConnectorId(stringId.toString());
    }

    private void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
        InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodePath(egress.getValue());
        TransmitPacketInput input = new TransmitPacketInputBuilder() //
                .setPayload(payload) //
                .setNode(new NodeRef(egressNodePath)) //
                .setEgress(egress) //
                .setIngress(ingress) //
                .build();
        packetProcessingService.transmitPacket(input);
    }*/

    public void sendToSwitch(JSONObject json) {
        /*Here we have to write all the code to send packet to the switch*/
        System.out.println("Send to switch --------");
        Integer inport = ((Long) json.get("inport")).intValue();
        Integer outport = ((Long) json.get("outport")).intValue();

        Integer swtch = ((Long) json.get("switch")).intValue();
        Integer dstport = ((Long) json.get("dstport")).intValue();
        Integer srcport = ((Long) json.get("srcport")).intValue();


        //////////////
        /// Payload
        List<Long> raw = (List<Long>) json.get("raw");
        StringBuffer sb = new StringBuffer("");
        for(Long b:raw){
            sb.append(b.byteValue());
        }
        byte[] payload = sb.toString().getBytes();
        ///////////////////

        ///////////////////
        /// Egress - dstMac
        /*List<Long> dstMacRaw = (List<Long>) json.get("dstmac");
        StringBuffer sbdstmac = new StringBuffer("");
        for(Long b:dstMacRaw){
            sbdstmac.append(b.byteValue());
        }
        byte[] dstMac = sbdstmac.toString().getBytes();

        NodeConnectorRef egress = mac2portMapping.get(dstMac); // ok? estaba mal ->  es null*/
        //////////////////////

        ///////////////////
        ///
        /*List<Long> srcMacRaw = (List<Long>) json.get("srcmac");
        StringBuffer sbsrcmac = new StringBuffer("");
        for(Long b:dstMacRaw){
            sbsrcmac.append(b.byteValue());
        }
        byte[] srcMac = sbsrcmac.toString().getBytes();

        NodeConnectorRef ingress = mac2portMapping.get(srcMac);*/

        /////////////////////////

        // FIXME port name and node id?
        // TODO
        //// We create the egress node
        NodeConnectorId egressConnectorId = new NodeConnectorId(swtch.toString());
        NodeId egressNodeId = new NodeId(inport.toString());
        InstanceIdentifier<NodeConnector> egressInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(egressNodeId))
                .child(NodeConnector.class, new NodeConnectorKey(egressConnectorId)).toInstance();
        NodeConnectorRef egress = new NodeConnectorRef(egressInstanceIdentifier);
        ////


        //// We create the ingress node
        NodeConnectorId ingressConnectorId = new NodeConnectorId(swtch.toString());
        NodeId ingressNodeId = new NodeId(outport.toString());
        InstanceIdentifier<NodeConnector> ingressInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(ingressNodeId))
                .child(NodeConnector.class, new NodeConnectorKey(ingressConnectorId)).toInstance();
        NodeConnectorRef ingress = new NodeConnectorRef(ingressInstanceIdentifier);
        ////

        InstanceIdentifier<Node> egressNodePath = InstanceIdentifierUtils.getNodePath(egress.getValue());

        TransmitPacketInput packet_out = new TransmitPacketInputBuilder().
                setPayload(payload).
                setNode(new NodeRef(egressNodePath)).
                setEgress(egress).
                setIngress(ingress).
                build();


        System.out.println("<----------- DEBUG -------------->");
        System.out.println("Egress: " + egress.getValue());
        System.out.println("Ingress: " + ingress.getValue());
        System.out.println("outport: " + outport +
                            ", switch: " + swtch + ", raw: " + raw);



        packetProcessingService.transmitPacket(packet_out);

    }

    public void setBackendChannel(BackendChannel channel)
    {
        this.channel = channel;
    }
}
