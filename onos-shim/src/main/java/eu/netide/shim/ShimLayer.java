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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.osgi.service.component.ComponentContext;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * ONOS shim application.
 */
@Component(immediate = true)
public class ShimLayer {

    private static final String DEFAULT_CORE_ADDRESS = "localhost";
    private static final int DEFAULT_CORE_PORT = 5555;

    private final Logger log = getLogger(getClass());

    private static int xId = 1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ApplicationId appId;

    private ZeroMQBaseConnector coreConnector;

    private NetIDEDeviceListener ofDeviceListener;

    @Property(name = "coreAddress", value = DEFAULT_CORE_ADDRESS, label = "Default CORE address is localhost")
    private String coreAddress = DEFAULT_CORE_ADDRESS;

    @Property(name = "corePort", intValue = DEFAULT_CORE_PORT, label = "Default CORE port is 5555")
    private Integer corePort = DEFAULT_CORE_PORT;


    @Property(name = "ipv6Forwarding", boolValue = false, label = "Enable IPv6 forwarding; default is false")
    private boolean ipv6Forwarding = false;

    @Activate
    public void activate(ComponentContext context) {

        appId = coreService.registerApplication("eu.netide.shim");
        cfgService.registerProperties(getClass());

        //ZeroMQ connection handler
        coreConnector = new ZeroMQBaseConnector();
        coreConnector.setAddress(coreAddress);
        coreConnector.setPort(corePort);

        readComponentConfiguration(context, false);

        //ShimController
        NetIDEShimController shimLayer = new NetIDEShimController(coreConnector, controller);
        coreConnector.RegisterCoreListener(shimLayer);

        //OFDevice initialization
        ofDeviceListener = new NetIDEDeviceListener(controller, shimLayer);
        controller.addListener(ofDeviceListener);
        controller.addMessageListener(ofDeviceListener);
        controller.addPacketListener(1, ofDeviceListener);

        //TODO: This is not the best way to do it, but NetIDE protocol do not handle different OF versions together
        for (OpenFlowSwitch sw : controller.getSwitches()) {
            Dpid dpid = new Dpid(sw.getId());
            controller.setRole(dpid, RoleState.MASTER);
            OFVersion version = sw.factory().getVersion();
            Integer ofVersion = version.getWireVersion();
            shimLayer.setSupportedProtocol(ofVersion.byteValue());
        }

        log.info("Starting the NetIDE Shim...");
        coreConnector.Start();
        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        coreConnector.Stop();
        cfgService.unregisterProperties(getClass(), false);
        flowRuleService.removeFlowRulesById(appId);
        controller.removeListener(ofDeviceListener);
        controller.removeMessageListener(ofDeviceListener);
        controller.removePacketListener(ofDeviceListener);
        ofDeviceListener = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        // TODO revoke unnecessary packet requests when config being modified
        readComponentConfiguration(context, true);
    }

    public static int getXId() {
        int current = xId;
        xId++;
        return current;
    }


    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context, boolean modified) {
        Dictionary<?, ?> properties = context.getProperties();

        boolean ipv6ForwardingEnabled = isPropertyEnabled(properties,
                                                          "ipv6Forwarding");
        if (ipv6Forwarding != ipv6ForwardingEnabled) {
            ipv6Forwarding = ipv6ForwardingEnabled;
            log.info("Configured. IPv6 forwarding is {}",
                     ipv6Forwarding ? "enabled" : "disabled");
        }

        String coreAddressProperty = get(properties,"coreAddress");

        String newCoreAddress = isNullOrEmpty(coreAddressProperty) ?
                DEFAULT_CORE_ADDRESS : coreAddressProperty;

        Integer newCorePort = getIntegerProperty(properties, "corePort");

        if (newCorePort == null) {
            newCorePort = DEFAULT_CORE_PORT;
        }

        if (!Objects.equals(newCoreAddress, coreAddress) || !Objects.equals(newCorePort, corePort)) {
            coreAddress = newCoreAddress;
            corePort = newCorePort;
            if (modified) {
                coreConnector.Stop();
                coreConnector.setAddress(coreAddress);
                coreConnector.setPort(corePort);
                coreConnector.Start();
            } else {
                coreConnector.setAddress(coreAddress);
                coreConnector.setPort(corePort);
            }
        }
    }

    /**
     * Check property name is defined and set to true.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return true when the propertyName is defined and set to true
     */
    private static boolean isPropertyEnabled(Dictionary<?, ?> properties,
                                             String propertyName) {
        boolean enabled = false;
        try {
            String flag = (String) properties.get(propertyName);
            if (flag != null) {
                enabled = flag.trim().equals("true");
            }
        } catch (ClassCastException e) {
            // No propertyName defined.
            enabled = false;
        }
        return enabled;
    }

    /**
     * Get Integer property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    private static Integer getIntegerProperty(Dictionary<?, ?> properties,
                                              String propertyName) {
        Integer value = null;
        try {
            String s = (String) properties.get(propertyName);
            value = isNullOrEmpty(s) ? value : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            value = null;
        }
        return value;
    }

}
