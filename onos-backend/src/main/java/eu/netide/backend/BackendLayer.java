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

import com.google.common.collect.Lists;
import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.javatuples.Pair;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample reactive forwarding application.
 */
@Component(immediate = true)
public class BackendLayer {

    private static final String DEFAULT_CORE_ADDRESS = "localhost";
    private static final int DEFAULT_CORE_PORT = 5555;
    private static final NetIDEProtocolVersion NETIDE_PROTOCOL_VERSION = NetIDEProtocolVersion.VERSION_1_4;
    public static final String MODULE_NAME = "backend-onos";
    public static final String ONOS_APP_NAME = "org.onosproject.fwd";

    private static int xId = 1;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry flowRuleProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketProviderRegistry packetProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ApplicationId appId;

    private ZeroMQBaseConnector coreConnector;

    private IModuleHandler moduleHandler;

    private NetIDEDeviceProvider deviceProvider;

    private NetIDEFlowRuleProvider flowRuleProvider;

    private NetIDEBackendController backendController;

    private NetIDEPacketProvider packetProvider;

    private List<Pair<Protocol, ProtocolVersions>> supportedProtocols;

    @Property(name = "coreAddress", value = DEFAULT_CORE_ADDRESS, label = "Default CORE address is localhost")
    private String coreAddress = DEFAULT_CORE_ADDRESS;

    @Property(name = "corePort", intValue = DEFAULT_CORE_PORT, label = "Default CORE port is 5555")
    private Integer corePort = DEFAULT_CORE_PORT;

    private ExecutorService backendTaskExecutor = Executors.newFixedThreadPool(5);

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("eu.netide.backend");

        //ZeroMQ connection handler
        coreConnector = new ZeroMQBaseConnector();
        coreConnector.setAddress(coreAddress);
        coreConnector.setPort(corePort);

        readComponentConfiguration(context, false);


        //ModuleHandler instantiation
        moduleHandler = new ModuleHandlerImpl(coreConnector, NETIDE_PROTOCOL_VERSION);

        //BackendController
        backendController =
                new NetIDEBackendController(coreConnector, moduleHandler, NETIDE_PROTOCOL_VERSION);

        coreConnector.RegisterModuleListener(moduleHandler);
        coreConnector.RegisterCoreListener(backendController);

        //DeviceProvider registration
        deviceProvider = new NetIDEDeviceProvider();
        DeviceProviderService deviceProviderService = deviceProviderRegistry.register(deviceProvider);
        deviceProvider.RegisterProviderService(deviceProviderService);
        backendController.setNetIDEDeviceProvider(deviceProvider);
        deviceProvider.setBackendController(backendController);
        deviceProvider.setModuleHandler(moduleHandler);

        //FlowRuleProvider registration
        flowRuleProvider = new NetIDEFlowRuleProvider();
        FlowRuleProviderService flowRuleProviderService = flowRuleProviderRegistry.register(flowRuleProvider);
        flowRuleProvider.setFlowRuleProviderService(flowRuleProviderService);
        flowRuleProvider.setApplicationService(applicationService);
        flowRuleProvider.setBackendController(backendController);
        flowRuleProvider.setDeviceProvider(deviceProvider);
        flowRuleProvider.setModuleHandler(moduleHandler);
        flowRuleProvider.setDriverService(driverService);

        backendController.setNetIDEFlowRuleProvider(flowRuleProvider);


        //TODO: PacketProvider registration

        packetProvider = new NetIDEPacketProvider();
        packetProvider.setBackendController(backendController);
        packetProvider.setModuleHandler(moduleHandler);
        packetProvider.setNetIDEDeviceProvider(deviceProvider);
        PacketProviderService packetService = packetProviderRegistry.register(packetProvider);
        packetProvider.setPacketProviderService(packetService);

        backendController.setNetIDEPacketProvider(packetProvider);

        supportedProtocols = Lists.newArrayList();
        supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_0));
        supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_3));

        backendController.setSupportedProtocol(supportedProtocols);

        log.info("Starting the NetIDE Backend...");
        coreConnector.Start();

        try {
            //wait for the socket setup
            Thread.sleep(1000);
        } catch (Exception e) {

        }


        moduleHandler.obtainBackendModuleId(getXId(), MODULE_NAME);

        Set<Application> runningApplications =  applicationService.getApplications();
        /*for (Application application : runningApplications) {
            if (applicationService.getState(application.id()) == ApplicationState.ACTIVE) {
                moduleHandler.obtainModuleId(getXId(), application.id().name(), moduleHandler.getModuleId(MODULE_NAME));
            }
        }*/
        moduleHandler.obtainModuleId(getXId(), ONOS_APP_NAME, moduleHandler.getModuleId(MODULE_NAME));
        startHandshakeMessage();

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        coreConnector.Stop();
        deviceProvider.deleteDevices();
        deviceProviderRegistry.unregister(deviceProvider);
        flowRuleProviderRegistry.unregister(flowRuleProvider);
        packetProviderRegistry.unregister(packetProvider);
        flowRuleProvider = null;
        deviceProvider = null;
        backendController = null;
        moduleHandler = null;
        packetProvider = null;
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    private void startHandshakeMessage() {
        HelloMessage hello = new HelloMessage();
        hello.getHeader().setDatapathId(-1);
        hello.getHeader().setPayloadLength((short) (supportedProtocols.size() * 2));
        hello.getHeader().setModuleId(moduleHandler.getModuleId(MODULE_NAME));
        hello.setSupportedProtocols(supportedProtocols);
        coreConnector.SendData(hello.toByteRepresentation());
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context, true);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context, boolean modified) {
        Dictionary<?, ?> properties = context.getProperties();

        String coreAddressProperty = get(properties,"coreAddress");

        String corePortProperty = get(properties,"corePort");

        String newCoreAddress = isNullOrEmpty(coreAddressProperty) ?
                DEFAULT_CORE_ADDRESS : coreAddressProperty;

        Integer newCorePort;

        try {
            newCorePort = isNullOrEmpty(corePortProperty) ?
                    DEFAULT_CORE_PORT : Integer.parseInt(corePortProperty.trim());
        } catch (NumberFormatException e) {
            return;
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

    public static int getXId() {
        int current = xId;
        xId++;
        return current;
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

}
