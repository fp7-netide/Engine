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
import eu.netide.backend.util.OpenFlowCorePacketContext;
import eu.netide.lib.netip.FenceMessage;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.PACKET_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by antonio on 10/10/16.
 */
public class NetIDEPacketProvider extends AbstractProvider implements PacketProvider {

    private final Logger log = getLogger(getClass());

    private PacketProviderService packetProviderService;
    private NetIDEDeviceProvider netIDEDeviceProvider;
    private NetIDEBackendController backendController;
    private IModuleHandler moduleHandler;

    private Cache<Integer, Integer> xids;

    public NetIDEPacketProvider() {
        super(new ProviderId("of", "eu.netide.provider.openflow"));
        this.xids = createBatchCache();
    }

    public void setPacketProviderService(PacketProviderService packetProviderService) {
        this.packetProviderService = packetProviderService;
    }

    public void setNetIDEDeviceProvider(NetIDEDeviceProvider deviceProvider) {
        this.netIDEDeviceProvider = deviceProvider;
    }

    public void setBackendController(NetIDEBackendController controller) {
        this.backendController = controller;
    }
    public void setModuleHandler(IModuleHandler moduleHandler) {
        this.moduleHandler = moduleHandler;
    }
    private Cache<Integer, Integer> createBatchCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS).build();
    }

    @Override
    public void emit(OutboundPacket packet) {

        DeviceId devId = packet.sendThrough();
        String scheme = devId.toString().split(":")[0];
        int backendModuleId = moduleHandler.getModuleId(BackendLayer.MODULE_NAME);

        if (!scheme.equals(this.id().scheme())) {
            throw new IllegalArgumentException(
                    "Don't know how to handle Device with scheme " + scheme);
        }

        Dpid dpid = Dpid.dpid(devId.uri());

        OFFactory outSwitchFactory = netIDEDeviceProvider.getSwitchOFFactory(dpid);

        if (outSwitchFactory == null) {
            log.warn("Device {} isn't available?", devId);
            return;
        }

        //Ethernet eth = new Ethernet();
        //eth.deserialize(packet.data().array(), 0, packet.data().array().length);
        OFPortDesc p = null;
        for (Instruction inst : packet.treatment().allInstructions()) {
            if (inst.type().equals(Instruction.Type.OUTPUT)) {
                p = portDesc(((Instructions.OutputInstruction) inst).port());
                OFPacketOut po = packetOut(outSwitchFactory, packet.data().array(), p.getPortNo());
                backendController.sendOpenFlowMessageToCore(po, BackendLayer.getXId(), dpid.value(), backendModuleId);
            }
        }
    }

    public void createPacketContext(Dpid device, OFPacketIn pkt, int transactionId, int moduleId) {

        DeviceId id = DeviceId.deviceId(Dpid.uri(device));

        OFFactory outSwitchFactory = netIDEDeviceProvider.getSwitchOFFactory(device);

        Ethernet ethPkt = parseEthernet(pkt);

        DefaultInboundPacket inPkt = new DefaultInboundPacket(
                new ConnectPoint(id, PortNumber.portNumber(pktinInPort(pkt).getPortNumber())),
                ethPkt, ByteBuffer.wrap(pkt.getData().clone()),
                cookie(pkt));

        DefaultOutboundPacket outPkt = new DefaultOutboundPacket(id, null,
                                                                 ByteBuffer.wrap(pkt.getData().clone()));;
        /*if (pkt.getBufferId() == OFBufferId.NO_BUFFER) {
            outPkt = new DefaultOutboundPacket(id, null,
                                               ByteBuffer.wrap(pkt.getData().clone()));
        }*/

        OpenFlowCorePacketContext corePktCtx =
                new OpenFlowCorePacketContext(System.currentTimeMillis(),
                                              inPkt, outPkt, false, outSwitchFactory, backendController,
                                              moduleId, Optional.of(pkt.getBufferId()), transactionId);
        packetProviderService.processPacket(corePktCtx);

        if (ethPkt != null) {
            if (corePktCtx.isHandled()) {
                backendController.sendFenceMessage(transactionId, moduleId);
            }
        }
    }

    private Ethernet parseEthernet(OFPacketIn pkt) {
        try {
            return Ethernet.deserializer().deserialize(pkt.getData(), 0, pkt.getData().length);
        } catch (BufferUnderflowException | NullPointerException |
                DeserializationException e) {
            Logger log = LoggerFactory.getLogger(getClass());
            log.error("packet deserialization problem : {}", e.getMessage());
            return null;
        }
    }

    private OFPortDesc portDesc(PortNumber port) {
        OFPortDesc.Builder builder = OFFactoryVer10.INSTANCE.buildPortDesc();
        builder.setPortNo(OFPort.of((int) port.toLong()));

        return builder.build();
    }

    private OFPacketOut packetOut(OFFactory factory, byte[] eth, OFPort out) {
        OFPacketOut.Builder builder = factory.buildPacketOut();
        OFAction act = factory.actions()
                .buildOutput()
                .setPort(out)
                .build();
        return builder
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(OFPort.CONTROLLER)
                .setActions(Collections.singletonList(act))
                .setData(eth)
                .build();
    }

    public Optional<Long> cookie(OFPacketIn pktin) {
        if (pktin.getVersion() != OFVersion.OF_10) {
            return Optional.of(pktin.getCookie().getValue());
        } else {
            return Optional.empty();
        }
    }

    private OFPort pktinInPort(OFPacketIn pktin) {
        if (pktin.getVersion() == OFVersion.OF_10) {
            return pktin.getInPort();
        }
        return pktin.getMatch().get(MatchField.IN_PORT);
    }
}
