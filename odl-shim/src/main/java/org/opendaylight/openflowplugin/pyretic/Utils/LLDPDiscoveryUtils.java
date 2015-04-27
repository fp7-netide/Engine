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
/*
* Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.openflowplugin.pyretic.Utils;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.liblldp.*;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.LLDP;
import org.opendaylight.controller.liblldp.LLDPTLV;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Created by Jennifer Hernández Bécares
 */
public class LLDPDiscoveryUtils {

    static Logger LOG = LoggerFactory.getLogger(LLDPDiscoveryUtils.class);
    public static final Long LLDP_INTERVAL = (long) (1000*5); // Send LLDP every five seconds
    public static final Long LLDP_EXPIRATION_TIME = LLDP_INTERVAL*3; // Let up to three intervals pass before we decide we are expired.

    private static final String OF_URI_PREFIX = "openflow:";


    /**
     *
     * @param payload
     * @return
     */
    public static NodeConnectorRef lldpToNodeConnectorRef(byte[] payload) {
        Ethernet ethPkt = new Ethernet();
        try {
            ethPkt.deserialize(payload, 0,payload.length * NetUtils.NumBitsInAByte);
        } catch (Exception e) {
            LOG.warn("Failed to decode LLDP packet {}", e);
        }
        if (ethPkt.getPayload() instanceof LLDP) {
            LLDP lldp = (LLDP) ethPkt.getPayload();
            try {
                NodeId srcNodeId = null;
                NodeConnectorId srcNodeConnectorId = null;
                for (LLDPTLV lldptlv : lldp.getOptionalTLVList()) {
                    if (lldptlv.getType() == LLDPTLV.TLVType.Custom.getValue()) {
                        srcNodeConnectorId = new NodeConnectorId(LLDPTLV.getCustomString(lldptlv.getValue(), lldptlv.getLength()));
                    }
                    if (lldptlv.getType() == LLDPTLV.TLVType.SystemName.getValue()) {
                        String srcNodeIdString = new String(lldptlv.getValue(),Charset.defaultCharset());
                        srcNodeId = new NodeId(srcNodeIdString);
                    }
                }
                if(srcNodeId != null && srcNodeConnectorId != null) {
                    InstanceIdentifier<NodeConnector> srcInstanceId = InstanceIdentifier.builder(Nodes.class)
                            .child(Node.class,new NodeKey(srcNodeId))
                            .child(NodeConnector.class, new NodeConnectorKey(srcNodeConnectorId))
                            .toInstance();
                    return new NodeConnectorRef(srcInstanceId);
                }
            } catch (Exception e) {
                LOG.warn("Caught exception ", e);
            }
        }
        return null;
    }


    /**
     * We use this function to create an LLDP packet in response to an inject_discovery_packet
     * (from Pyretic)
     * Check out https://github.com/opendaylight/openflowplugin/blob/master/applications/lldp-speaker/src/main/java/org/opendaylight/openflowplugin/applications/lldpspeaker/LLDPUtil.java
     * for more info on how to handle the creation of LLDP packets
     * @param nodeIdWithPrefix
     * @param outPort
     * @param srcAddress
     * @return
     */
    public synchronized static TransmitPacketInput createLLDPOut(final String nodeIdWithPrefix,
                                                                   final String outPort,
                                                                   final MacAddress srcAddress) {

        TransmitPacketInputBuilder tPackBuilder = new TransmitPacketInputBuilder();
        LLDP lldp = new LLDP();
        Ethernet eth = new Ethernet();

        // We remove the prefix from the nodeID
        String nodeId = nodeIdWithPrefix.replace(OF_URI_PREFIX, "");

        // Creates the chassisId associated to the nodeId as LLDPTLV
        BigInteger dataPathId = dataPathIdFromNodeId(nodeId);
        byte[] cidValue = LLDPTLV
                .createChassisIDTLVValue(colonize(bigIntegerToPaddedHex(dataPathId)));
        LLDPTLV chassisIdTlv = new LLDPTLV();
        chassisIdTlv.setType(LLDPTLV.TLVType.ChassisID.getValue());
        chassisIdTlv.setType(LLDPTLV.TLVType.ChassisID.getValue())
                .setLength((short) cidValue.length).setValue(cidValue);

        // Creates and sets the portIdTLV as LLDPTLV
        String hexString = Integer.toHexString(Integer.parseInt(outPort));
        byte[] portID = LLDPTLV.createPortIDTLVValue(hexString);
        LLDPTLV portIdTlv = new LLDPTLV();
        portIdTlv.setType(LLDPTLV.TLVType.PortID.getValue())
                .setLength((short) portID.length).setValue(portID);

        // Sets the chassisId and portId to the LLDP packet
        lldp.setChassisId(chassisIdTlv);
        lldp.setPortId(portIdTlv);

        // Set ethernet type as LLDP, source mac from the in port and LLDP Multicast as dst
        // Then, we set the payload to be the payload from the LLDP packet we created before
        eth.setEtherType((short)0x88cc);
        eth.setSourceMACAddress(srcAddress.getValue().getBytes());
        eth.setDestinationMACAddress(LLDP.LLDPMulticastMac);
        eth.setPayload(lldp.getPayload());

        // We create a Node connector associated to the nodeId
        NodeRef ref = OutputUtils.createNodeRef(nodeId);
        NodeConnectorRef nEgressConfRef = new NodeConnectorRef(OutputUtils.createNodeConnRef(nodeId, outPort));

        // We create the action list of the packet (output packet to outPort)
        // Similar to how we create action lists in OutputUtils (packetOut)
        // The difference is where we send the packets (Uri changes)
        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(Integer.valueOf(0xffff));
        Uri value = new Uri(outPort);
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        tPackBuilder.setConnectionCookie(null);
        tPackBuilder.setAction(actionList);
        tPackBuilder.setPayload(eth.getRawPayload());
        tPackBuilder.setNode(ref);
        tPackBuilder.setEgress(nEgressConfRef);
        tPackBuilder.setBufferId(Long.valueOf(0xffffffffL));

        return tPackBuilder.build();
    }

    // Auxiliary functions
    private static BigInteger dataPathIdFromNodeId(String nodeId) {
        String dpids = nodeId.replace(OF_URI_PREFIX, "");
        return new BigInteger(dpids);
    }

    private static String colonize(String orig) {
        return orig.replaceAll("(?<=..)(..)", ":$1");
    }

    private static String bigIntegerToPaddedHex(BigInteger dataPathId) {
        return StringUtils.leftPad(dataPathId.toString(16), 16, "0");
    }

    public static String macToString(byte[] mac) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            b.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
        }
        return b.toString();
    }
    // End of auxiliary functions
}