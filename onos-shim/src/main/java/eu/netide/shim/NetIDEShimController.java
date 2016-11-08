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
import eu.netide.lib.netip.OpenFlowMessage;
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
import org.projectfloodlight.openflow.protocol.OFConfigFlags;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFDescStatsRequest;
import org.projectfloodlight.openflow.protocol.OFEchoReply;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFGetConfigRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFSetConfig;
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

    private Map<Dpid,NetIDESwitch> switchMap;

    public NetIDEShimController (ZeroMQBaseConnector connector, OpenFlowController controller) {
        coreConnector = connector;
        this.controller = controller;
        this.xids = Maps.newHashMap();
        this.switchMap = Maps.newHashMap();
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
        log.debug("Dpid {}", dpid);
        OpenFlowSwitch sw = controller.getSwitch(dpid);

        if (sw == null) {
            log.error("Switch {} disconnected", dpid);
            return;
        }

        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(msg.array());
        OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();
        try {
            OFMessage message = reader.readFrom(buffer);
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
                    OFStatsRequest reply = (OFStatsRequest) message;
                    switch (reply.getStatsType()) {
                        case DESC:
                            OFDescStatsRequest ofDescStatsRequest = (OFDescStatsRequest) reply;
                            OFDescStatsReply.Builder ofDescReply = sw.factory().buildDescStatsReply();
                            ofDescReply.setXid(ofDescStatsRequest.getXid());
                            sendOpenFlowMessageToCore(ofDescReply.build(),ofDescReply.getXid(),sw.getId(),moduleId);
                            break;
                        default:
                            //Save the xid
                            xids.put(message.getXid(), moduleId);
                            sw.sendMsg(message);
                            break;
                    }
                    break;

                case BARRIER_REQUEST:
                    xids.put(message.getXid(), moduleId);
                    sw.sendMsg(message);
                    break;

                case ECHO_REQUEST:

                    OFEchoRequest echoRequest = (OFEchoRequest) message;
                    ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
                    echoRequest.writeTo(buf);
                    byte[] payload = buf.array();
                    OFEchoReply.Builder echoReply = sw.factory().buildEchoReply();
                    echoReply.setXid(echoRequest.getXid());
                    echoReply.setData(payload);
                    sendOpenFlowMessageToCore(echoReply.build(),echoReply.getXid(),sw.getId(),moduleId);
                    break;

                case FEATURES_REQUEST:
                    OFFeaturesReply featuresReply = getFeatureReply(sw);
                    sendOpenFlowMessageToCore(featuresReply,featuresReply.getXid(),sw.getId(),moduleId);

                    //Create OFPortDescStatsReply for OF_13
                    if (sw.factory().getVersion() == OFVersion.OF_13) {
                        OFPortDescStatsReply.Builder statsReplyBuilder = sw.factory().buildPortDescStatsReply();
                        statsReplyBuilder.setEntries(sw.getPorts())
                                .setXid(0);
                        OFPortDescStatsReply ofPortStatsReply = statsReplyBuilder.build();
                        sendOpenFlowMessageToCore(ofPortStatsReply,ofPortStatsReply.getXid(),sw.getId(),moduleId);
                    }
                    break;

                case PACKET_OUT:
                    sw.sendMsg(message);
                    break;

                case GET_CONFIG_REQUEST:
                    OFGetConfigRequest setConfig = (OFGetConfigRequest) message;
                    OFGetConfigReply.Builder configReply = sw.factory().buildGetConfigReply();
                    configReply.setXid(setConfig.getXid());
                    Set<OFConfigFlags> flags = Sets.newHashSet(OFConfigFlags.FRAG_NORMAL);
                    configReply.setFlags(flags);
                    configReply.setMissSendLen(0);
                    sendOpenFlowMessageToCore(configReply.build(),configReply.getXid(),sw.getId(),moduleId);
                    break;

                case SET_CONFIG:
                    OFSetConfig ofSetConfig = (OFSetConfig) message;
                    OFGetConfigReply.Builder ofGetConfigReply = sw.factory().buildGetConfigReply();
                    ofGetConfigReply.setXid(ofSetConfig.getXid());
                    Set<OFConfigFlags> flagsSet = Sets.newHashSet(OFConfigFlags.FRAG_NORMAL);
                    ofGetConfigReply.setFlags(flagsSet);
                    ofGetConfigReply.setMissSendLen(0);
                    //sendOpenFlowMessageToCore(ofGetConfigReply.build(),ofGetConfigReply.getXid(),sw.getId(),moduleId);
                    break;
                default:
                    //sw.sendMsg(message);
                    log.error("Unhandled OF message {}", message);
                    break;
            }

        } catch (Exception e) {
            log.error("Error in decoding OFMessage from the CORE {}", e);
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
        OpenFlowMessage message = new OpenFlowMessage();
        message.getHeader().setPayloadLength((short)payload.length);
        message.setOfMessage(msg);
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

