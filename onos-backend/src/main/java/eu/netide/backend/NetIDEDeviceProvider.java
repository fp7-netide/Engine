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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.PortSpeed;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.Port.Type.FIBER;
import static org.onosproject.openflow.controller.Dpid.dpid;
import static org.onosproject.openflow.controller.Dpid.uri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by antonio on 08/02/16.
 */
public class NetIDEDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final long KBPS = 1_000;
    private static final long MBPS = 1_000 * 1_000;

    private final Logger log = getLogger(getClass());

    private DeviceProviderService providerService;
    private NetIDEBackendController backendController;
    private IModuleHandler moduleHandler;

    private Map<Dpid, NetIDESwitch> switchMap = Maps.newHashMap();


    public NetIDEDeviceProvider() {
        super(new ProviderId("of", "eu.netide.provider.openflow"));
    }

    public void RegisterProviderService(DeviceProviderService deviceProviderService) {
        this.providerService = deviceProviderService;
    }

    public void setBackendController(NetIDEBackendController backendController) {
        this.backendController = backendController;
    }

    public void setModuleHandler(IModuleHandler moduleHandler) {
        this.moduleHandler = moduleHandler;
    }

    public OFFactory getSwitchOFFactory(Dpid dpid) {
        if (switchMap.containsKey(dpid)) {
            NetIDESwitch netIDESwitch = switchMap.get(dpid);
            return netIDESwitch.factory();
        } else {
            return null;
        }
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {

        log.debug("Triggering probe on device {}", deviceId);

        final Dpid dpid = dpid(deviceId.uri());
        NetIDESwitch ofSwitch = switchMap.get(dpid);
        if (ofSwitch == null) {
            log.error("Failed to probe device {} on sw={}", deviceId);
            providerService.deviceDisconnected(deviceId);
            return;
        } else {
            log.trace("Confirmed device {} connection", deviceId);
        }

        // Prompt an update of port information. We can use any XID for this.
        OFFactory fact = ofSwitch.factory();
        switch (fact.getVersion()) {
            case OF_10:
                backendController.sendOpenFlowMessageToCore(fact.buildFeaturesRequest().setXid(0).build(), 0, dpid.value(), moduleHandler.getModuleId(BackendLayer.MODULE_NAME));
                break;
            case OF_13:
                backendController.sendOpenFlowMessageToCore(fact.buildPortDescStatsRequest().setXid(0).build(),  0, dpid.value(), moduleHandler.getModuleId(BackendLayer.MODULE_NAME));
                break;
            default:
                log.warn("Unhandled protocol version");
        }

    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {

    }

    public void registerNewSwitch(Dpid dpid, OFFeaturesReply msg) {
        if (providerService == null) {
            return;
        }
        NetIDESwitch ofSwitch = new NetIDESwitch(dpid, msg, msg.getVersion());

        if(ofSwitch.getVersion() == OFVersion.OF_10) {
            //We have port description inside feature reply

            DeviceId did = deviceId(uri(dpid));

            ChassisId cId = new ChassisId(dpid.value());

            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PROTOCOL, msg.getVersion().toString())
                    .build();

            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), Device.Type.SWITCH,
                                                 "none",
                                                 "none",
                                                 "none",
                                                 "none",
                                                 cId, annotations);
            switchMap.put(dpid, ofSwitch);
            providerService.deviceConnected(did, description);
            providerService.updatePorts(did, buildPortDescriptions(msg.getPorts()));
        } else {
            switchMap.put(dpid, ofSwitch);
        }

    }

    public void registerSwitchPorts(Dpid dpid, OFPortDescStatsReply portDesc) {
        //We register an OF 1.3 switch in the core after the OFPortDescStatsReply message
        //We have port description inside feature reply

        if (switchMap.containsKey(dpid)) {
            NetIDESwitch ofSwitch = switchMap.get(dpid);

            DeviceId did = deviceId(uri(dpid));

            ChassisId cId = new ChassisId(dpid.value());

            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PROTOCOL, ofSwitch.getVersion().toString())
                    .build();

            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), Device.Type.SWITCH,
                                                 "none",
                                                 "none",
                                                 "none",
                                                 "none",
                                                 cId, annotations);
            providerService.deviceConnected(did, description);
            providerService.updatePorts(did, buildPortDescriptions(portDesc.getEntries()));
        }

    }

    /**
     * Build a portDescription from a given Ethernet port description.
     *
     * @param port the port to build from.
     * @return portDescription for the port.
     */
    private PortDescription buildPortDescription(OFPortDesc port) {
        PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
        boolean enabled =
                !port.getState().contains(OFPortState.LINK_DOWN) &&
                        !port.getConfig().contains(OFPortConfig.PORT_DOWN);
        Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
        SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString());
        return new DefaultPortDescription(portNo, enabled, type,
                                          portSpeed(port), annotations);
    }

    /**
     * Creates an annotation for the port name if one is available.
     *
     * @param portName the port name
     * @param portMac the port mac
     * @return annotation containing the port name if one is found,
     *         null otherwise
     */
    private SparseAnnotations makePortAnnotation(String portName, String portMac) {
        SparseAnnotations annotations = null;
        String pName = Strings.emptyToNull(portName);
        String pMac = Strings.emptyToNull(portMac);
        if (portName != null) {
            annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PORT_NAME, pName)
                    .set(AnnotationKeys.PORT_MAC, pMac).build();
        }
        return annotations;
    }

    private long portSpeed(OFPortDesc port) {
        if (port.getVersion() == OFVersion.OF_13) {
            // Note: getCurrSpeed() returns a value in kbps (this also applies to OF_11 and OF_12)
            return port.getCurrSpeed() / KBPS;
        }

        PortSpeed portSpeed = PortSpeed.SPEED_NONE;
        for (OFPortFeatures feat : port.getCurr()) {
            portSpeed = PortSpeed.max(portSpeed, feat.getPortSpeed());
        }
        return portSpeed.getSpeedBps() / MBPS;
    }

    private PortDescription buildPortDescription(OFPortStatus status) {
        OFPortDesc port = status.getDesc();
        if (status.getReason() != OFPortReason.DELETE) {
            return buildPortDescription(port);
        } else {
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
            SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString());
            return new DefaultPortDescription(portNo, false, type,
                                              portSpeed(port), annotations);
        }
    }

    /**
     * Builds a list of port descriptions for a given list of ports.
     *
     * @return list of portdescriptions
     */
    private List<PortDescription> buildPortDescriptions(List<OFPortDesc> ports) {

        final List<PortDescription> portDescs = new ArrayList<>(ports.size());
        ports.forEach(port -> portDescs.add(buildPortDescription(port)));
        return portDescs;
    }

    public void portChanged(Dpid dpid, OFPortStatus status) {
        log.debug("portChanged({},{})", dpid, status);
        PortDescription portDescription = buildPortDescription(status);
        providerService.portStatusChanged(deviceId(uri(dpid)), portDescription);
    }

    public void deleteDevices() {
        for (Dpid sw : switchMap.keySet()) {
            NetIDESwitch ofswitch = switchMap.get(sw);
            DeviceId did = deviceId(uri(ofswitch.getDpid()));
            providerService.deviceDisconnected(did);
        }
    }
}
