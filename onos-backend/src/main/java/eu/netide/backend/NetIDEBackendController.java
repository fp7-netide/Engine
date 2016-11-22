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

package eu.netide.backend;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import eu.netide.lib.netip.FenceMessage;
import eu.netide.lib.netip.HeartbeatMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageHeader;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import io.netty.buffer.ByteBuf;
import org.javatuples.Pair;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onlab.packet.Ethernet;
import org.onosproject.net.DeviceId;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class handles the messages from the Core and dispatches them to ONOS core services
 */

public class NetIDEBackendController implements ICoreListener {

    private final int HEARTBEAT_TIMER = 3;

    private NetIDEProtocolVersion netIpVersion;

    private List<Pair<Protocol, ProtocolVersions>> supportedProtocols;
    private final Logger log = getLogger(getClass());

    private ZeroMQBaseConnector coreConnector;
    private IModuleHandler moduleHandler;
    private NetIDEDeviceProvider netIDEDeviceProvider;
    private NetIDEFlowRuleProvider netIDEFlowRuleProvider;
    private NetIDEPacketProvider netIDEPacketProvider;

    private Cache<Integer, Integer> xids;
    private AtomicInteger lastXid;
    //private List<Integer> xids = Lists.newArrayList();

    private ScheduledExecutorService heartBeatTaskExecutor = Executors.newScheduledThreadPool(1);

    public NetIDEBackendController(ZeroMQBaseConnector connector, IModuleHandler moduleHandler, NetIDEProtocolVersion netIpVersion) {
        coreConnector = connector;
        this.moduleHandler = moduleHandler;
        this.netIpVersion = netIpVersion;
        this.xids = createBatchCache();
        this.lastXid = new AtomicInteger();
    }

    public List<Pair<Protocol, ProtocolVersions>> getSupportedProtocolsBackend() {
        return this.supportedProtocols;
    }

    public void setSupportedProtocol(List<Pair<Protocol, ProtocolVersions>> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    public void setNetIDEDeviceProvider(NetIDEDeviceProvider deviceProvider) {
        this.netIDEDeviceProvider = deviceProvider;
    }

    public void setNetIDEFlowRuleProvider(NetIDEFlowRuleProvider flowRuleProvider) {
        this.netIDEFlowRuleProvider = flowRuleProvider;
    }

    public void setNetIDEPacketProvider(NetIDEPacketProvider netIDEPacketProvider) {
        this.netIDEPacketProvider = netIDEPacketProvider;
    }

    public AtomicInteger getLastXid() {
        return this.lastXid;
    }

    private Cache<Integer, Integer> createBatchCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS).build();
    }

    @Override
    public void onOpenFlowCoreMessage(Long datapathId, ByteBuf msg, int moduleId, int transactionId) {

        Dpid dpid = new Dpid(datapathId);
        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));

        /*if (netIDEDeviceProvider.getSwitchOFFactory(dpid) == null) {
            //The switch does not exist, check if needed
            return;
        }*/

        //Check module ID, multibackend case
        if (moduleId != moduleHandler.getModuleId(BackendLayer.MODULE_NAME) && moduleHandler.getModuleNameFromID(moduleId) != null) {

            ChannelBuffer buffer = ChannelBuffers.copiedBuffer(msg.array());
            OFMessageReader<OFMessage> reader = OFFactories.getGenericReader();
            try {
                OFMessage message = reader.readFrom(buffer);
                log.debug("Msg from NetIDE core: TransactionId {}, ModuleId {}, OpenFlow Message {}", transactionId, moduleId, message);
                switch (message.getType()) {
                    case PORT_STATUS:
                        netIDEDeviceProvider.portChanged(dpid, (OFPortStatus) message);
                        break;
                    case PACKET_IN:
                        Integer xid = xids.getIfPresent(transactionId);
                        if (xid != null) {
                            //TODO: Send only fence?
                            //sendFenceMessage(transactionId, moduleId);
                        } else {
                            xids.put(transactionId, transactionId);
                            lastXid.set(transactionId);
                            netIDEPacketProvider.createPacketContext(dpid, (OFPacketIn) message, transactionId, moduleId);
                        }
                        break;
                    case FLOW_REMOVED:
                        OFFlowRemoved flowRemoved = (OFFlowRemoved) message;
                        netIDEFlowRuleProvider.notifyFlowRemoved(deviceId, flowRemoved);
                        break;
                    case FEATURES_REPLY:
                        //Send to DeviceProvider
                        netIDEDeviceProvider.registerNewSwitch(dpid, (OFFeaturesReply) message);
                        break;
                    case STATS_REPLY:
                        OFStatsReply reply = (OFStatsReply) message;
                        switch (reply.getStatsType()) {
                            case PORT_DESC:
                                //Handling OF 13 port desc
                                netIDEDeviceProvider.registerSwitchPorts(dpid, (OFPortDescStatsReply) reply);
                                break;
                            case FLOW:
                                netIDEFlowRuleProvider.notifyStatistics(deviceId, (OFFlowStatsReply) message);
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }

            } catch (Exception e) {
                log.error("Error in decoding OFMessage from the CORE {}", e);
            }
        }
    }

    @Override
    public void onHelloCoreMessage(List<Pair<Protocol, ProtocolVersions>> supportedProtocols, int moduleId) {

        for (Pair<Protocol, ProtocolVersions> receivedFromCore : supportedProtocols) {
            log.info("Pair {}", receivedFromCore);

            for (Pair<Protocol, ProtocolVersions> supportedProtocolsBackend : getSupportedProtocolsBackend()) {
                if (receivedFromCore.getValue0().getValue() == supportedProtocolsBackend.getValue0().getValue()
                        && receivedFromCore.getValue1().getValue() == supportedProtocolsBackend.getValue1().getValue()) {

                    heartBeatTaskExecutor.scheduleAtFixedRate(() -> sendHeartBeat(), HEARTBEAT_TIMER, HEARTBEAT_TIMER, TimeUnit.SECONDS);
                    log.info("Handshake Completed");
                    return;

                }
            }
        }
        log.error("No match found on protocol version with the Core");
    }

    private void sendHeartBeat() {
        HeartbeatMessage msg = new HeartbeatMessage();
        //msg.getHeader().setNetIDEProtocolVersion(netIpVersion);
        msg.getHeader().setModuleId(moduleHandler.getModuleId(BackendLayer.MODULE_NAME));
        msg.getHeader().setPayloadLength((short) 0);
        msg.getHeader().setDatapathId(-1);
        msg.getHeader().setTransactionId(BackendLayer.getXId());
        coreConnector.SendData(msg.toByteRepresentation());
    }

    public void sendOpenFlowMessageToCore(OFMessage msg, int xId,
                                          long datapathId, int moduleId) {

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        msg.writeTo(buf);
        byte[] payload = buf.array();
        OpenFlowMessage message = new OpenFlowMessage();
        message.getHeader().setPayloadLength((short)payload.length);
        message.setOfMessage(msg);
        message.getHeader().setDatapathId(datapathId);
        message.getHeader().setModuleId(moduleId);
        message.getHeader().setTransactionId(xId);
        coreConnector.SendData(message.toByteRepresentation());
    }

    public void sendFenceMessage(int transactionId, int moduleId) {
        FenceMessage fence = new FenceMessage();
        fence.getHeader().setModuleId(moduleId);
        fence.getHeader().setPayloadLength((short) 0);
        fence.getHeader().setDatapathId(-1);
        fence.getHeader().setTransactionId(transactionId);
        coreConnector.SendData(fence.toByteRepresentation());
    }

}

