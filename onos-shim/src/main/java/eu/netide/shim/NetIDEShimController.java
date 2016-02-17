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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import io.netty.buffer.ByteBuf;
import org.javatuples.Pair;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;


public class NetIDEShimController implements ICoreListener {

    private OpenFlowController controller;

    private static final int ONOS_DEFAULT_PRIORITY = 5;

    private Pair<Protocol, ProtocolVersions> supportedProtocol;
    private final Logger log = getLogger(getClass());

    private ZeroMQBaseConnector coreConnector;

    private HashMap<Long, Integer> xids;

    public NetIDEShimController (ZeroMQBaseConnector connector, OpenFlowController controller) {
        coreConnector = connector;
        this.controller = controller;
        this.xids = Maps.newHashMap();
    }

    public Pair<Protocol, ProtocolVersions> getSupportedProtocol() {
        return this.supportedProtocol;
    }

    public void setSupportedProtocol(byte version) {
        this.supportedProtocol = new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW,
                                                                      ProtocolVersions.parse(Protocol.OPENFLOW, version));
    }

    public Integer getAndDeleteModuleId (Long xid) {
        return this.xids.remove(xid);
    }

    public boolean containsXid (Long xid) {
        return this.xids.containsKey(xid);
    }

    @Override
    public void onOpenFlowCoreMessage(Long datapathId, ByteBuf msg, int moduleId) {

        //TODO: Handle messages that requires replies (e.g. barrier)
        Dpid dpid = new Dpid(datapathId);
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(msg.array());
        OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();
        try {
            OFMessage message = reader.readFrom(buffer);
            log.debug("Msg from core: {}",message);
            switch (message.getType()) {
                case FLOW_MOD:
                    OFFlowMod flowMod = (OFFlowMod) message;
                    if (flowMod.getPriority() > ONOS_DEFAULT_PRIORITY) {
                        sw.sendMsg(message);
                    } else {
                        OFFlowMod.Builder flowModBuilder = flowMod.createBuilder();
                        flowModBuilder.setPriority(ONOS_DEFAULT_PRIORITY+1);
                        sw.sendMsg(flowModBuilder.build());
                    }
                    break;
                case STATS_REQUEST:
                    //Save the xid
                    xids.put(message.getXid(), moduleId);
                    break;
                case BARRIER_REQUEST:
                    //Save the xid
                    xids.put(message.getXid(), moduleId);;
                    break;
                case ECHO_REQUEST:
                    xids.put(message.getXid(), moduleId);
                    break;
                default:
                    sw.sendMsg(message);
            }

        } catch (Exception e) {
            log.error("Error in decoding OFMessage from the CORE");
        }
    }

    @Override
    public void onHelloCoreMessage(List<Pair<Protocol, ProtocolVersions>> requestedProtocols, int moduleId) {

        for (Pair<Protocol, ProtocolVersions> requested : requestedProtocols) {
            if (getSupportedProtocol() != null) {
                if (requested.getValue0().getValue() == getSupportedProtocol().getValue0().getValue()
                        && requested.getValue1().getValue() == getSupportedProtocol().getValue1().getValue()) {
                    HelloMessage msg = new HelloMessage();
                    log.info("Supported protocols {}", getSupportedProtocol());
                    msg.getSupportedProtocols().add(getSupportedProtocol());
                    msg.getHeader().setPayloadLength((short) 2);
                    msg.getHeader().setModuleId(moduleId);
                    coreConnector.SendData(msg.toByteRepresentation());
                    log.debug("NetIDE HELLO message sent to the core...");

                    for (OpenFlowSwitch sw : controller.getSwitches()) {

                        OFFeaturesReply featuresReply = getFeatureReply(sw);
                        sendOpenFlowMessageToCore(featuresReply,featuresReply.getXid(),sw.getId(),moduleId);

                        //Create OFPortDescStatsReply for OF_13
                        if (sw.factory().getVersion() == OFVersion.OF_13) {
                            OFPortDescStatsReply.Builder statsReplyBuilder = sw.factory().buildPortDescStatsReply();
                            statsReplyBuilder.setEntries(sw.getPorts())
                                    .setXid(0);
                            OFPortDescStatsReply ofPortStatsReply = statsReplyBuilder.build();
                            sendOpenFlowMessageToCore(ofPortStatsReply,ofPortStatsReply.getXid(),sw.getId(),0);
                        }
                    }

                }
            }
        }
    }

    public void sendOpenFlowMessageToCore(OFMessage msg, long xId,
                                          long datapathId, int moduleId) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        msg.writeTo(buf);
        byte[] payload = buf.array();
        Message message = new Message(NetIPUtils.StubHeaderFromPayload(payload), payload);
        message.getHeader().setMessageType(MessageType.OPENFLOW);
        message.getHeader().setDatapathId(datapathId);
        message.getHeader().setModuleId(moduleId);
        message.getHeader().setTransactionId((int) xId);
        coreConnector.SendData(message.toByteRepresentation());
    }

    public OFFeaturesReply getFeatureReply(OpenFlowSwitch dev) {
        //TODO: handle different OF versions
        OFFactory factory = dev.factory();

        OFFeaturesReply.Builder fea = factory.buildFeaturesReply();
        fea.setDatapathId(DatapathId.of(dev.getId()));
        fea.setXid(0);
        if (factory.getVersion() == OFVersion.OF_10) {
            Set<OFActionType> actions = new HashSet<>();
            actions.add(OFActionType.COPY_TTL_IN);
            actions.add(OFActionType.COPY_TTL_OUT);
            actions.add(OFActionType.ENQUEUE);
            actions.add(OFActionType.OUTPUT);
            actions.add(OFActionType.SET_DL_DST);
            actions.add(OFActionType.SET_DL_SRC);
            actions.add(OFActionType.SET_NW_DST);
            actions.add(OFActionType.SET_NW_SRC);
            actions.add(OFActionType.SET_NW_TOS);
            actions.add(OFActionType.SET_VLAN_VID);
            actions.add(OFActionType.SET_VLAN_PCP);
            actions.add(OFActionType.STRIP_VLAN);
            actions.add(OFActionType.SET_TP_DST);
            actions.add(OFActionType.SET_TP_SRC);
            fea.setActions(actions);
            fea.setPorts(dev.getPorts());
        }
        Set<OFCapabilities> capabilities = new HashSet<OFCapabilities>();
        capabilities.add(OFCapabilities.FLOW_STATS);
        capabilities.add(OFCapabilities.PORT_STATS);
        capabilities.add(OFCapabilities.QUEUE_STATS);
        fea.setCapabilities(capabilities);
        fea.setNBuffers(4096);

        return fea.build();
    }

}

