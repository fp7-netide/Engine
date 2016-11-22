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

import org.onlab.packet.Ip4Address;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.Instructions.SetQueueInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanPcpInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionEnqueue;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Flow mod builder for OpenFlow 1.0.
 */
public class FlowModBuilderVer10 extends FlowModBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final int OFPCML_NO_BUFFER = 0xffff;

    private final TrafficTreatment treatment;

    /**
     * Constructor for a flow mod builder for OpenFlow 1.0.
     *
     * @param flowRule the flow rule to transform into a flow mod
     * @param factory the OpenFlow factory to use to build the flow mod
     * @param xid the transaction ID
     * @param driverService the device driver service
     */
    protected FlowModBuilderVer10(FlowRule flowRule,
                                  OFFactory factory, Optional<Long> xid,
                                  Optional<DriverService> driverService) {
        super(flowRule, factory, xid, driverService);

        this.treatment = flowRule.treatment();
    }

    @Override
    public OFFlowAdd buildFlowAdd() {
        Match match = buildMatch();
        List<OFAction> actions = buildActions();

        long cookie = flowRule().id().value();


        OFFlowAdd.Builder fm = factory().buildFlowAdd()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(actions)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority());
        
        if (flowRule().timeout() != 0) {
            fm.setIdleTimeout(flowRule().timeout());
        } else {
            fm.setHardTimeout(flowRule().hardTimeout());
        }

        return fm.build();
    }

    @Override
    public OFFlowMod buildFlowMod() {
        Match match = buildMatch();
        List<OFAction> actions = buildActions();

        long cookie = flowRule().id().value();

        OFFlowMod.Builder fm = factory().buildFlowModify()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setActions(actions)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority())
                .setPriority(flowRule().priority());

        if (flowRule().timeout() != 0) {
            fm.setIdleTimeout(flowRule().timeout());
        } else {
            fm.setHardTimeout(flowRule().hardTimeout());
        }

        return fm.build();
    }

    @Override
    public OFFlowDelete buildFlowDel() {
        Match match = buildMatch();

        long cookie = flowRule().id().value();

        OFFlowDelete.Builder fm = factory().buildFlowDelete()
                .setXid(xid)
                .setCookie(U64.of(cookie))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setMatch(match)
                .setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM))
                .setPriority(flowRule().priority());

        if (flowRule().timeout() != 0) {
            fm.setIdleTimeout(flowRule().timeout());
        } else {
            fm.setHardTimeout(flowRule().hardTimeout());
        }

        return fm.build();
    }

    private List<OFAction> buildActions() {
        List<OFAction> acts = new LinkedList<>();
        OFAction act;
        if (treatment == null) {
            return acts;
        }
        for (Instruction i : treatment.immediate()) {
            switch (i.type()) {
            case NOACTION:
                return Collections.emptyList();
            case L2MODIFICATION:
                act = buildL2Modification(i);
                if (act != null) {
                    acts.add(buildL2Modification(i));
                }
                break;
            case L3MODIFICATION:
                act = buildL3Modification(i);
                if (act != null) {
                    acts.add(buildL3Modification(i));
                }
                break;
            case OUTPUT:
                OutputInstruction out = (OutputInstruction) i;
                OFActionOutput.Builder action = factory().actions().buildOutput()
                        .setPort(OFPort.of((int) out.port().toLong()));
                if (out.port().equals(PortNumber.CONTROLLER)) {
                    action.setMaxLen(OFPCML_NO_BUFFER);
                }
                acts.add(action.build());
                break;
            case QUEUE:
                SetQueueInstruction queue = (SetQueueInstruction) i;
                if (queue.port() == null) {
                    log.warn("Required argument 'port' undefined for OFActionEnqueue");
                }
                OFActionEnqueue.Builder queueBuilder = factory().actions().buildEnqueue()
                        .setQueueId(queue.queueId())
                        .setPort(OFPort.ofInt((int) queue.port().toLong()));
                acts.add(queueBuilder.build());
                break;
            case L0MODIFICATION:
            case L1MODIFICATION:
            case GROUP:
            case TABLE:
            case METADATA:
                log.warn("Instruction type {} not supported with protocol version {}",
                        i.type(), factory().getVersion());
                break;
            default:
                log.warn("Instruction type {} not yet implemented.", i.type());
            }
        }

        return acts;
    }

    private OFAction buildL3Modification(Instruction i) {
        L3ModificationInstruction l3m = (L3ModificationInstruction) i;
        ModIPInstruction ip;
        Ip4Address ip4;
        switch (l3m.subtype()) {
        case IPV4_SRC:
            ip = (ModIPInstruction) i;
            ip4 = ip.ip().getIp4Address();
            return factory().actions().setNwSrc(IPv4Address.of(ip4.toInt()));
        case IPV4_DST:
            ip = (ModIPInstruction) i;
            ip4 = ip.ip().getIp4Address();
            return factory().actions().setNwDst(IPv4Address.of(ip4.toInt()));
        default:
            log.warn("Unimplemented action type {}.", l3m.subtype());
            break;
        }
        return null;
    }

    private OFAction buildL2Modification(Instruction i) {
        L2ModificationInstruction l2m = (L2ModificationInstruction) i;
        ModEtherInstruction eth;
        switch (l2m.subtype()) {
        case ETH_DST:
            eth = (ModEtherInstruction) l2m;
            return factory().actions().setDlDst(MacAddress.of(eth.mac().toLong()));
        case ETH_SRC:
            eth = (ModEtherInstruction) l2m;
            return factory().actions().setDlSrc(MacAddress.of(eth.mac().toLong()));
        case VLAN_ID:
            ModVlanIdInstruction vlanId = (ModVlanIdInstruction) l2m;
            return factory().actions().setVlanVid(VlanVid.ofVlan(vlanId.vlanId().toShort()));
        case VLAN_PCP:
            ModVlanPcpInstruction vlanPcp = (ModVlanPcpInstruction) l2m;
            return factory().actions().setVlanPcp(VlanPcp.of(vlanPcp.vlanPcp()));
        case VLAN_POP:
            return factory().actions().stripVlan();
        case VLAN_PUSH:
            return null;
        default:
            log.warn("Unimplemented action type {}.", l2m.subtype());
            break;
        }
        return null;
    }

}
