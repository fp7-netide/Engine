/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.floodlightcontroller.interceptor;

import eu.netide.lib.netip.FenceMessage;
import eu.netide.lib.netip.HeartbeatMessage;
import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import eu.netide.lib.netip.Protocol;
import eu.netide.lib.netip.ProtocolVersions;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.SwitchStatus;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packetstreamer.thrift.OFMessageType;

import org.javatuples.Pair;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class NetIdeModule implements IFloodlightModule, IOFSwitchListener, IOFMessageListener, ICoreListener, Runnable {
    protected IFloodlightProviderService floodlightProvider;
    protected static Logger logger;
    protected ZeroMQBaseConnector coreConnector;
    private OFVersion aggreedVersion;
    private List<Pair<Protocol, ProtocolVersions>> supportedProtocols;
    private IModuleHandler moduleHandler;
    private Map<Long, ChannelFuture> managedSwitchesChannel = new HashMap<Long, ChannelFuture>();
    private Map<Long, ClientBootstrap> managedBootstraps = new HashMap<Long, ClientBootstrap>();
    private Map<Long, DummySwitch> managedSwitches = new HashMap<Long, DummySwitch>();
    private int xId = 1;
    private boolean handshake = false;
    private NetIDEProtocolVersion netIpVersion = NetIDEProtocolVersion.VERSION_1_3;
    private Pair<Protocol, ProtocolVersions> protocolMatched;
    private Thread helloThread = new Thread(this);
    private final String moduleName = "floodlight-backend";
    private final int heartbeatTimeout = 5000;
    private final OFType[] handshakeMessages = { OFType.HELLO, OFType.FEATURES_REPLY, OFType.GET_CONFIG_REPLY,
    		OFType.STATS_REPLY, OFType.ROLE_REPLY, OFType.ECHO_REPLY, OFType.ECHO_REQUEST };
    
    private List<String> modules;
    
    private int getXId() {
        int current = xId;
        xId++;
        return current;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.IListener#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.floodlightcontroller.core.IListener#isCallbackOrderingPrereq(java.
     * lang.Object, java.lang.String)
     */
    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.floodlightcontroller.core.IListener#isCallbackOrderingPostreq(java.
     * lang.Object, java.lang.String)
     */
    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.IOFMessageListener#receive(net.
     * floodlightcontroller.core.IOFSwitch,
     * org.projectfloodlight.openflow.protocol.OFMessage,
     * net.floodlightcontroller.core.FloodlightContext)
     */
    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        logger.info("Message received from controller: " + msg.getType());

        if (!sw.getStatus().equals(SwitchStatus.HANDSHAKE)) {
            managedSwitches.get(sw.getId().getLong()).setHandshakeCompleted(true);
        }
        
        if (msg.getType().equals(OFMessageType.FLOW_MOD) || msg.getType().equals(OFMessageType.PACKET_OUT)){
        	FenceMessage fence = new FenceMessage();
            fence.getHeader().setNetIDEProtocolVersion(netIpVersion);
            fence.getHeader().setModuleId(moduleHandler.getModuleId(-1, moduleName));
            fence.getHeader().setPayloadLength((short) 0);
            fence.getHeader().setDatapathId(-1);
            fence.getHeader().setTransactionId(Relay.getNetIpID());
            coreConnector.SendData(fence.toByteRepresentation());
        }
        
        return Command.CONTINUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchAdded(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchAdded(DatapathId switchId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchRemoved(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchRemoved(DatapathId switchId) {
        managedSwitches.remove(switchId.getLong());
        managedBootstraps.remove(switchId.getLong());
    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchActivated(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchActivated(DatapathId switchId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.floodlightcontroller.core.IOFSwitchListener#switchPortChanged(org.
     * projectfloodlight.openflow.types.DatapathId,
     * org.projectfloodlight.openflow.protocol.OFPortDesc,
     * net.floodlightcontroller.core.PortChangeType)
     */
    @Override
    public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.IOFSwitchListener#switchChanged(org.
     * projectfloodlight.openflow.types.DatapathId)
     */
    @Override
    public void switchChanged(DatapathId switchId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.floodlightcontroller.core.module.IFloodlightModule#getModuleServices(
     * )
     */
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.floodlightcontroller.core.module.IFloodlightModule#getServiceImpls()
     */
    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.module.IFloodlightModule#init(net.
     * floodlightcontroller.core.module.FloodlightModuleContext)
     */
    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        supportedProtocols = new ArrayList<>();
        supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_0));
        supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_3));
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        logger = LoggerFactory.getLogger(NetIdeModule.class);
        coreConnector = new ZeroMQBaseConnector(supportedProtocols);
        String coreIp = context.getConfigParams(this).getOrDefault("coreIp","127.0.0.1");
        coreConnector.setAddress(coreIp);
        int corePort;
        try{
        	corePort = Integer.valueOf(context.getConfigParams(this).getOrDefault("corePort", "5555"));
        }catch(java.lang.NumberFormatException e){
        	corePort = 5555;
        }
        
        coreConnector.setPort(corePort);
        coreConnector.RegisterCoreListener(this);
        moduleHandler = new ModuleHandlerImpl(coreConnector);
        coreConnector.RegisterModuleListener(moduleHandler);
        coreConnector.Start();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.floodlightcontroller.core.module.IFloodlightModule#startUp(net.
     * floodlightcontroller.core.module.FloodlightModuleContext)
     */
    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
    	modules = new ArrayList<>();
    	modules.addAll(0, 
    			Arrays.asList(context.getConfigParams(this).getOrDefault("modules", "floodlight-backend").split(",")));
        initListeners();
        
        for (String module : modules){
        	
        	moduleHandler.obtainModuleId(getXId(), module);
        }
        
        helloThread.start();
    }

    private void initListeners() {
        floodlightProvider.addOFMessageListener(OFType.HELLO, this);
        floodlightProvider.addOFMessageListener(OFType.ECHO_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.ERROR, this);
        floodlightProvider.addOFMessageListener(OFType.EXPERIMENTER, this);
        floodlightProvider.addOFMessageListener(OFType.FEATURES_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.GET_CONFIG_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.SET_CONFIG, this);
        floodlightProvider.addOFMessageListener(OFType.PACKET_OUT, this);
        floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.PORT_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.GROUP_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.STATS_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.BARRIER_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.TABLE_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.QUEUE_GET_CONFIG_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.ROLE_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.GET_ASYNC_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.SET_ASYNC, this);
        floodlightProvider.addOFMessageListener(OFType.METER_MOD, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.floodlightcontroller.interceptor.CoreListener#onOpenFlowCoreMessage(
     * java.lang.Long, io.netty.buffer.ByteBuf, int)
     */
    @Override
    public void onOpenFlowCoreMessage(Long datapathId, OFMessage msg, int moduleId) {
        if (!(managedSwitches.containsKey(datapathId)) || msg.getType().equals(OFType.FEATURES_REPLY)) {
        	logger.debug("Adding switch datapathID: " + datapathId.toString());
            OFFeaturesReply features = null;
            if (msg.getType().equals(OFType.FEATURES_REPLY)) {
                features = (OFFeaturesReply) msg;
            }
            aggreedVersion = msg.getVersion();
            DummySwitch dummySwitch = new DummySwitch(datapathId, features);
            addNewSwitch(dummySwitch);

        } else if (managedSwitches.get(datapathId).isHandshakeCompleted()
                || Arrays.asList(handshakeMessages).contains(msg.getType())) {
        	if (msg.getXid() == managedSwitches.get(datapathId).nextXid || managedSwitches.get(datapathId).isHandshakeCompleted()){
        		logger.debug("Message received from the core for switch DataPathID: " + 
                        datapathId.toString() + " Type: " + msg.getType().toString() + "XID: " + msg.getXid());
        		Relay.sendToController(managedSwitchesChannel.get(datapathId), msg);
        		managedSwitches.get(datapathId).lastXid = msg.getXid();
        		managedSwitches.get(datapathId).nextXid = -1;
        	}else {
        		logger.debug("Unexpected message received from the core for switch DataPathID: " + 
                        datapathId.toString() + " Type: " + msg.getType().toString() + " XID: " + msg.getXid() +
                        " Dropping it.");
        	}
        }else{
        	logger.debug("Unexpected message received from the core for switch DataPathID: " + 
                    datapathId.toString() + " Type: " + msg.getType().toString() + " XID: " + msg.getXid() +
                    " Dropping it.");
        }
    }

    private void addNewSwitch(DummySwitch dummySwitch) {
        final SwitchChannelHandler switchHandler = new SwitchChannelHandler(coreConnector, aggreedVersion);
        switchHandler.setDummySwitch(dummySwitch); // CONTAINS ALL THE INFO
                                                   // ABOUT THIS SWITCH

        ChannelFactory factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() {
                return Channels.pipeline(switchHandler);
            }
        });

        // CONNECT AND ADD TO HASHMAP OF MANAGED SWITCHES
        ChannelFuture future = bootstrap.connect(new InetSocketAddress("localhost", 7753));
        managedSwitchesChannel.put(dummySwitch.getDatapathId(), future);
        managedBootstraps.put(dummySwitch.getDatapathId(), bootstrap);
        managedSwitches.put(dummySwitch.getDatapathId(), dummySwitch);
        switchHandler.registerSwitchConnection(future, bootstrap);
        switchHandler.setModuleHandler(moduleHandler);

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.floodlightcontroller.interceptor.CoreListener#onHelloCoreMessage(java
     * .util.List, int)
     */

    @Override
    public void onHelloCoreMessage(List<Pair<Protocol, ProtocolVersions>> requiredVersion, int moduleId) {
        logger.info("HEllo from core");
        matchNetIpVersion(requiredVersion);
    }

    private void matchNetIpVersion(List<Pair<Protocol, ProtocolVersions>> requiredVersion) {
        logger.info("Handskake complete");
        handshake = true;
        protocolMatched = requiredVersion.get(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            startNetIpHandshake();
//            /sendHeartbeat();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void sendHeartbeat() throws InterruptedException {
        HeartbeatMessage msg = new HeartbeatMessage();
        msg.getHeader().setNetIDEProtocolVersion(netIpVersion);
        msg.getHeader().setModuleId(moduleHandler.getModuleId(-1, moduleName));
        msg.getHeader().setPayloadLength((short) 0);
        msg.getHeader().setDatapathId(-1);
        while (true) {
            msg.getHeader().setTransactionId(Relay.getNetIpID());
            coreConnector.SendData(msg.toByteRepresentation());
            Thread.sleep(heartbeatTimeout); 
        }
    }

    private void startNetIpHandshake() throws InterruptedException {
        HelloMessage hello = new HelloMessage();
        hello.getHeader().setDatapathId(-1);
        hello.getHeader().setPayloadLength((short) (supportedProtocols.size() * 2));
        hello.getHeader().setNetIDEProtocolVersion(netIpVersion);
        hello.getHeader().setModuleId(moduleHandler.getModuleId(-1, moduleName));
        hello.setSupportedProtocols(supportedProtocols);
        boolean version_agreed = false;
        while (!version_agreed) {
            hello.getHeader().setTransactionId(Relay.getNetIpID());
            coreConnector.SendData(hello.toByteRepresentation());
            Thread.sleep(3000);
            if (handshake)
                version_agreed = true;
        }
    }
}