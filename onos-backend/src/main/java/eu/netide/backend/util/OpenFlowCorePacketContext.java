/*
 *  Copyright (c) 2016, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 *  Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 *  Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 *  Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors: Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.backend.util;

import eu.netide.backend.BackendLayer;
import eu.netide.backend.NetIDEBackendController;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instruction.Type;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Packet context used with the OpenFlow providers.
 */
public class OpenFlowCorePacketContext extends DefaultPacketContext {

    private static final Logger log = LoggerFactory.getLogger(OpenFlowCorePacketContext.class);

    private OFFactory ofFactory;
    private NetIDEBackendController controller;
    private int backendModuleId;
    private OFBufferId bufferId;
    private Integer lastxid;

    /**
     * Creates a new OpenFlow core packet context.
     *
     * @param time creation time
     * @param inPkt inbound packet
     * @param outPkt outbound packet
     * @param block whether the context is blocked or not
     */
    public OpenFlowCorePacketContext(long time, InboundPacket inPkt,
                                     OutboundPacket outPkt, boolean block, OFFactory factory,
                                     NetIDEBackendController controller, int backendModuleId, Optional<OFBufferId> bufferId,
                                     Integer lastxid) {
        super(time, inPkt, outPkt, block);
        this.ofFactory = factory;
        this.controller = controller;
        this.backendModuleId = backendModuleId;
        this.bufferId = bufferId.orElse(OFBufferId.NO_BUFFER);
        this.lastxid = lastxid;

    }

    @Override
    public void send() {
        if (!this.block()) {
            if (outPacket() == null) {
                sendPacket(null);
            } else {
                try {
                    Ethernet eth = Ethernet.deserializer()
                            .deserialize(outPacket().data().array(), 0,
                                         outPacket().data().array().length);
                    sendPacket(eth);
                } catch (DeserializationException e) {
                    log.warn("Unable to deserialize packet");
                }
            }
        }
    }

    private void sendPacket(Ethernet eth) {
        List<Instruction> ins = treatmentBuilder().build().allInstructions();
        OFPort p = null;
        //TODO: support arbitrary list of treatments must be supported in ofPacketContext
        for (Instruction i : ins) {
            if (i.type() == Type.OUTPUT) {
                p = buildPort(((OutputInstruction) i).port());
                break; //for now...
            }
        }

        Dpid id = Dpid.dpid(this.inPacket().receivedFrom().deviceId().uri());

        OFPacketOut packetOut = packetOut(ofFactory, eth.serialize(), p);
        controller.sendOpenFlowMessageToCore(packetOut, lastxid, id.value(), backendModuleId);
    }

    private OFPort buildPort(PortNumber port) {
        return OFPort.of((int) port.toLong());
    }

    private OFPacketOut packetOut(OFFactory factory, byte[] eth, OFPort out) {
        OFPacketOut.Builder builder = factory.buildPacketOut();
        OFAction act = factory.actions()
                .buildOutput()
                .setPort(out)
                .build();
        return builder
                .setBufferId(bufferId)
                .setInPort(buildPort(this.inPacket().receivedFrom().port()))
                .setActions(Collections.singletonList(act))
                .setData(eth)
                .build();
    }


}
