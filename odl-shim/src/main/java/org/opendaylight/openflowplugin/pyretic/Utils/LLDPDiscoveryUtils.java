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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.opendaylight.controller.liblldp.*;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.LLDP;
import org.opendaylight.controller.liblldp.LLDPTLV;
import org.opendaylight.controller.sal.packet.*;
import org.opendaylight.controller.sal.packet.address.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
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


    public static String macToString(byte[] mac) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            b.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
        }
        return b.toString();
    }
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
     * @param nodeId
     * @param outPort
     * @return
     * */
    public synchronized static TransmitPacketInput createLLDPOut(final String nodeId,
                                                                   final String outPort,
                                                                   final MacAddress srcAddress) {

        TransmitPacketInputBuilder tPackBuilder = new TransmitPacketInputBuilder();
        LLDP lldp = new LLDP();
        Ethernet eth = new Ethernet();

        LLDPTLV chassisId = new LLDPTLV();
        chassisId.setType(LLDPTLV.TLVType.ChassisID.getValue());
        chassisId.setValue(LLDPTLV.createChassisIDTLVValue(nodeId));
        chassisId.setLength((short)LLDPTLV.createChassisIDTLVValue(nodeId).length);


        String hexString = Integer.toHexString(Integer.parseInt(outPort));
        byte[] portID = LLDPTLV.createPortIDTLVValue(hexString);
        LLDPTLV portIdTlv = new LLDPTLV();
        portIdTlv.setType(LLDPTLV.TLVType.PortID.getValue())
                .setLength((short) portID.length).setValue(portID);


        lldp.setChassisId(chassisId);
        lldp.setPortId(portIdTlv);


        eth.setEtherType((short)0x88cc);
        eth.setSourceMACAddress(srcAddress.getValue().getBytes()); // FIXME?
        eth.setDestinationMACAddress(LLDP.LLDPMulticastMac);
        eth.setPayload(lldp.getPayload());


        NodeRef ref = OutputUtils.createNodeRef(nodeId);
        NodeConnectorRef nEgressConfRef = new NodeConnectorRef(OutputUtils.createNodeConnRef(nodeId, outPort));

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(Integer.valueOf(0xffff));
        Uri value = new Uri(outPort); // FIXME? Before it was OutputPortValues.NORMAL.toString()
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        tPackBuilder.setConnectionCookie(null);
        tPackBuilder.setAction(actionList);
        tPackBuilder.setPayload(eth.getRawPayload());
        tPackBuilder.setNode(ref);
        tPackBuilder.setEgress(nEgressConfRef); // FIXME Ingress not set
        tPackBuilder.setBufferId(Long.valueOf(0xffffffffL));

        return tPackBuilder.build();
    }
}