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

package eu.netide.shim;

import org.onlab.packet.Ethernet;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowMessageListener;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by antonio on 08/02/16.
 */
public class NetIDEDeviceListener implements OpenFlowSwitchListener, OpenFlowMessageListener, PacketListener {

    private final Logger log = getLogger(getClass());

    private OpenFlowController controller;

    private NetIDEShimController shimController;


    public NetIDEDeviceListener (OpenFlowController controller, NetIDEShimController shimController) {
        this.controller = controller;
        this.shimController = shimController;
    }

    @Override
    public void switchAdded(Dpid dpid) {
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        OFVersion version = sw.factory().getVersion();
        Integer ofVersion = version.getWireVersion();
        shimController.setSupportedProtocol(ofVersion.byteValue());
        OFFeaturesReply featuresReply = shimController.getFeatureReply(sw);
        controller.setRole(dpid, RoleState.MASTER);
        shimController.sendOpenFlowMessageToCore(featuresReply,featuresReply.getXid(),sw.getId(),0);

        //Create OFPortDescStatsReply for OF_13
        if (sw.factory().getVersion() == OFVersion.OF_13) {
            OFPortDescStatsReply.Builder statsReplyBuilder = sw.factory().buildPortDescStatsReply();
            statsReplyBuilder.setEntries(sw.getPorts())
                    .setXid(0);
            OFPortDescStatsReply ofPortStatsReply = statsReplyBuilder.build();
            shimController.sendOpenFlowMessageToCore(ofPortStatsReply,ofPortStatsReply.getXid(),sw.getId(),0);
        }
        log.info("Switch {} connected", dpid);
    }

    @Override
    public void switchRemoved(Dpid dpid) {
        //TODO: Notify to the core of a device disconnection
    }

    @Override
    public void switchChanged(Dpid dpid) {
        //This method is called when a FEATURES_REPLY arrives
        log.debug("Switch changed");
    }

    @Override
    public void portChanged(Dpid dpid, OFPortStatus status) {
        shimController.sendOpenFlowMessageToCore(status, status.getXid(), dpid.value(), 0);
    }

    @Override
    public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {

    }

    @Override
    public void handlePacket(OpenFlowPacketContext openFlowPacketContext) {

        if (!isControlPacket(openFlowPacketContext.parsed())) {
            openFlowPacketContext.block();
        }

    }

    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    @Override
    public void handleIncomingMessage(Dpid dpid, OFMessage msg) {

        switch (msg.getType()) {
            case PACKET_IN:
                shimController.sendOpenFlowMessageToCore(msg, ShimLayer.getXId(), dpid.value(), 0);
                break;
            case FLOW_REMOVED:
                shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), 0);
                break;
            case PORT_STATUS:
                shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), 0);
                break;
            case STATS_REPLY:
                if (shimController.containsXid(msg.getXid())) {
                    shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), shimController.getAndDeleteModuleId(msg.getXid()));
                }
                break;
            case ERROR:
                shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), 0);
                break;
            case BARRIER_REPLY:
                if (shimController.containsXid(msg.getXid())) {
                    shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), shimController.getAndDeleteModuleId(msg.getXid()));
                }
                break;
            case ECHO_REPLY:
                if (shimController.containsXid(msg.getXid())) {
                    shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), shimController.getAndDeleteModuleId(msg.getXid()));
                }
                break;
            default:
                if (shimController.containsXid(msg.getXid())) {
                    shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), shimController.getAndDeleteModuleId(msg.getXid()));
                } else {
                    shimController.sendOpenFlowMessageToCore(msg, msg.getXid(), dpid.value(), 0);
                }
                break;
        }
    }

    @Override
    public void handleOutgoingMessage(Dpid dpid, List<OFMessage> list) {

    }
}
