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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.openflowplugin.pyretic.Utils.FlowUtils;
import org.opendaylight.openflowplugin.pyretic.Utils.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
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

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.openflowplugin.pyretic.Utils.OutputUtils;


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
    private int switches = 0;

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

        System.out.println("Value: " + nodeId.getValue());



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


        // FIXME
        switches++;
        String p = "[\"switch\", \"join\", " + switches + ", \"BEGIN\"]";
        String p21 = "[\"port\", \"join\", " + switches + ", 1, true, false, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]";
        String p22 = "[\"port\", \"join\", " + switches + ", 2, true, false, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]";
        String p23 = "[\"port\", \"join\", " + switches + ", 3, true, false, [\"OFPPF_COPPER\", \"OFPPF_10GB_FD\"]]";
        String p3 = "[\"switch\", \"join\", " + switches + ", \"END\"]";
        this.channel.push(p);
        sleep();
        this.channel.push(p21);
        sleep();
        this.channel.push(p22);
        sleep();
        this.channel.push(p23);
        sleep();
        this.channel.push(p3);
        System.out.println(p + "\n" + p21 + "\n" + p22 + "\n" + p23 + "\n" + p3);

    }

    // new
    private void sleep() {
        try {
            Thread.sleep(1000);                 //1000 milliseconds is one second.
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
        System.out.println("-----New packet arrived");

        byte[] etherType = PacketUtils.extractEtherType(notification.getPayload());
        byte[] dstMacRaw = PacketUtils.extractDstMac(notification.getPayload());
        byte[] srcMacRaw = PacketUtils.extractSrcMac(notification.getPayload());

        MacAddress dstMac = PacketUtils.rawMacToMac(dstMacRaw);
        MacAddress srcMac = PacketUtils.rawMacToMac(srcMacRaw);


        // Debug
        System.out.println("srcmac init");
        System.out.println(srcMac.getValue());
        System.out.println("dstmac init");
        System.out.println(dstMac.getValue());


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

            System.out.print("Ethertype: " ); // + etherType.toString());
            for(int i = 0; i < etherType.length; i++) {
                System.out.printf("%02x ",0xff & etherType[i]);
            }
            System.out.println("");

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
                //mac2portMapping.put(srcMac, notification.getIngress());
            }
            else if (Arrays.equals(ETH_TYPE_IPV6, etherType)) {
                // Handle IPV6 packet
                JSONObject json = new JSONObject();

                json.put("switch", Integer.parseInt(switch_s));
                json.put("inport", Integer.parseInt(inport));
                json.put("raw",raw);

                List<String> p = new ArrayList<String>();
                p.add("\"packet\"");
                p.add(json.toString());
                System.out.println(p);
                this.channel.push(p.toString() + "\n");
                //mac2portMapping.put(srcMac, notification.getIngress());
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


                if (this.channel == null) System.out.println("Impossible to push");
                else {
                    System.out.println("Pushing arp packet");
                    System.out.println(p);
                }
                this.channel.push(p.toString() + "\n");
                mac2portMapping.put(srcMac, notification.getIngress());

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
            throw new IllegalArgumentException("String " + path + " does not contain -");
        }
    }


    public void setBackendChannel(BackendChannel channel)
    {
        this.channel = channel;
    }

    // basurilla
    static InstanceIdentifier<NodeConnector> createNodeConnectorId(String nodeKey, String nodeConnectorKey) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId(nodeKey)))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnectorKey)))
                .build();
    }
    //

    @Override
    public void sendToSwitch(JSONObject json, String type) {

        if (type.equals("packet")) {

            Integer swtch = ((Long) json.get("switch")).intValue();
            Integer inport = ((Long) json.get("inport")).intValue();
            Integer outport = ((Long) json.get("outport")).intValue();

            String inNodeKey = "openflow:" + swtch.toString();
            String inPort = inport.toString();
            String outPort = outport.toString(); //

            //System.out.println("innodekey: " + inNodeKey);
            //System.out.println("inport: " + inPort);
            //System.out.println("outport: " + outPort);

            ////////////////////////////////////////////////////////
            // Get the raw from the json
            List<Long> raw = (List<Long>) json.get("raw");
            StringBuilder sb = new StringBuilder("");
            for (Long b : raw) {
                sb.append(OutputUtils.fromDecimalToHex(b));
            }
            byte[] payload = OutputUtils.toByteArray(sb.toString());
            //System.out.println("My payload: " + sb.toString().toUpperCase());
            ////////////////////////////////////////////////////////

/*
            ////////////////////////////////////////////////////////
            // Get the source mac from the json
            raw = (List<Long>) json.get("srcmac");
            sb = new StringBuilder("");
            for (Long b : raw) {
                if (b != 58) sb.append((char)b.byteValue());
            }
            byte[] srcMac = OutputUtils.toByteArray(sb.toString());
            System.out.println("My sb src: " + sb.toString().toUpperCase());
            ////////////////////////////////////////////////////////


            ////////////////////////////////////////////////////////
            // Get the dst mac from the json
            raw = (List<Long>) json.get("dstmac");
            sb = new StringBuilder("");
            for (Long b : raw) {
                if (b != 58) sb.append((char)b.byteValue());
            }
            byte[] dstMac = OutputUtils.toByteArray(sb.toString());
            System.out.println("My sb dst: " + sb.toString().toUpperCase());
            ////////////////////////////////////////////////////////

            ////////////////////////////////////////////////////////
            // Get the src ip from the json
            raw = (List<Long>) json.get("srcip");
            sb = new StringBuilder("");
            for (Long b : raw) {
                if (b != 46) sb.append((char)b.byteValue());
            }
            byte[] srcip = OutputUtils.toByteArray(sb.toString());
            System.out.println("My src ip: " + sb.toString().toUpperCase());
            ////////////////////////////////////////////////////////


            ////////////////////////////////////////////////////////
            // Get the dst ip from the json
            raw = (List<Long>) json.get("dstip");
            sb = new StringBuilder("");
            for (Long b : raw) {
                if (b != 46) sb.append((char)b.byteValue());
            }
            byte[] dstip = OutputUtils.toByteArray(sb.toString());
            System.out.println("My dst ip: " + sb.toString().toUpperCase());
            ////////////////////////////////////////////////////////
*/
            /*
                Ethernet types in pyretic:
                             HEX    -> Decimal
                LLDP_TYPE  = 0x88cc -> 35020
                ARP_TYPE   = 0x806  -> 2054
                IP_TYPE    = 0x800  -> 2048
                IPV6_TYPE  = 0x86dd -> 34525
            */
            TransmitPacketInput input = null;
           // Integer ethtype = ((Long) json.get("ethtype")).intValue();
            // Create ARP packet out
            /*if (ethtype == 2054) {
                System.out.println("ARP");
            }
            else if (ethtype == 2048)
                System.out.println("IP");*/

            input = OutputUtils.createPacketOut(inNodeKey, payload,
                        outPort, inPort);
            //}
            // Create IP packet out
            //else if (ethtype == 2048) {

            //}


            packetProcessingService.transmitPacket(input);
            System.out.println("Transmitted <<<<");

        }

        else {
            System.out.println("Different type <<<<<< " + type);
        }
    }


}
