/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.shim;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.shim.message.NetIDEHeader;
import org.onosproject.shim.message.NetIDEMessage;
import org.onosproject.shim.message.NetIDEProtocol;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFeaturesRequest;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMatchV1;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFAuxId;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample reactive forwarding application.
 */
@Component(immediate = true)
public class ShimLayer {

	private static final int TCP_PORT = 41414;
	private static TCPServer tcpServer = null;
	private static int BUFFER_ID = 1;
	private HashMap<OFBufferId, PacketContext> inPackets = new HashMap();
	private static LinkedList<ClientController> clientCtrls = new LinkedList<ClientController>();

	private static final int DEFAULT_TIMEOUT = 10;
	private static final int DEFAULT_PRIORITY = 10;

	private final Logger log = getLogger(getClass());

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected TopologyService topologyService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected DeviceService deviceService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected PacketService packetService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected HostService hostService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected LinkService linkService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected FlowObjectiveService flowObjectiveService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected CoreService coreService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected ComponentConfigService cfgService;

	private ReactivePacketProcessor processor = new ReactivePacketProcessor();

	private ApplicationId appId;

	@Property(name = "packetOutOnly", boolValue = false, label = "Enable packet-out only forwarding; default is false")
	private boolean packetOutOnly = false;

	@Property(name = "packetOutOfppTable", boolValue = false, label = "Enable first packet forwarding using OFPP_TABLE port "
			+ "instead of PacketOut with actual port; default is false")
	private boolean packetOutOfppTable = false;

	@Property(name = "flowTimeout", intValue = DEFAULT_TIMEOUT, label = "Configure Flow Timeout for installed flow rules; "
			+ "default is 10 sec")
	private int flowTimeout = DEFAULT_TIMEOUT;

	@Property(name = "flowPriority", intValue = DEFAULT_PRIORITY, label = "Configure Flow Priority for installed flow rules; "
			+ "default is 10")
	private int flowPriority = DEFAULT_PRIORITY;

	@Property(name = "ipv6Forwarding", boolValue = false, label = "Enable IPv6 forwarding; default is false")
	private boolean ipv6Forwarding = false;

	@Property(name = "matchDstMacOnly", boolValue = false, label = "Enable matching Dst Mac Only; default is false")
	private boolean matchDstMacOnly = false;

	@Property(name = "matchVlanId", boolValue = false, label = "Enable matching Vlan ID; default is false")
	private boolean matchVlanId = false;

	@Property(name = "matchIpv4Address", boolValue = false, label = "Enable matching IPv4 Addresses; default is false")
	private boolean matchIpv4Address = false;

	@Property(name = "matchIpv4Dscp", boolValue = false, label = "Enable matching IPv4 DSCP and ECN; default is false")
	private boolean matchIpv4Dscp = false;

	@Property(name = "matchIpv6Address", boolValue = false, label = "Enable matching IPv6 Addresses; default is false")
	private boolean matchIpv6Address = false;

	@Property(name = "matchIpv6FlowLabel", boolValue = false, label = "Enable matching IPv6 FlowLabel; default is false")
	private boolean matchIpv6FlowLabel = false;

	@Property(name = "matchTcpUdpPorts", boolValue = false, label = "Enable matching TCP/UDP ports; default is false")
	private boolean matchTcpUdpPorts = false;

	@Property(name = "matchIcmpFields", boolValue = false, label = "Enable matching ICMPv4 and ICMPv6 fields; "
			+ "default is false")
	private boolean matchIcmpFields = false;

	@Activate
	public void activate(ComponentContext context) {
		cfgService.registerProperties(getClass());
		appId = coreService.registerApplication("org.onosproject.shim");

		packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 2);
		readComponentConfiguration(context);

		log.info("Starting the NetIDE Shim TCP Server on port " + TCP_PORT);
		tcpServer = new TCPServer(this, TCP_PORT);
		new Thread(tcpServer).start();
		requestPackests();
		log.info("Started with Application ID {}", appId.id());
	}

	@Deactivate
	public void deactivate() {
		// TODO revoke all packet requests when deactivate
		cfgService.unregisterProperties(getClass(), false);
		flowRuleService.removeFlowRulesById(appId);
		packetService.removeProcessor(processor);
		processor = null;
		tcpServer.stop();
		log.info("Stopped");
	}

	@Modified
	public void modified(ComponentContext context) {
		// TODO revoke unnecessary packet requests when config being modified
		readComponentConfiguration(context);
		requestPackests();
	}

	/**
	 * Request packet in via PacketService.
	 */
	private void requestPackests() {
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
		selector.matchEthType(Ethernet.TYPE_IPV4);
		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE,
				appId);
		selector.matchEthType(Ethernet.TYPE_ARP);
		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE,
				appId);

		if (ipv6Forwarding) {
			selector.matchEthType(Ethernet.TYPE_IPV6);
			packetService.requestPackets(selector.build(),
					PacketPriority.REACTIVE, appId);
		}
	}

	/**
	 * Extracts properties from the component configuration context.
	 *
	 * @param context
	 *            the component context
	 */
	private void readComponentConfiguration(ComponentContext context) {
		Dictionary<?, ?> properties = context.getProperties();
		boolean packetOutOnlyEnabled = isPropertyEnabled(properties,
				"packetOutOnly");
		if (packetOutOnly != packetOutOnlyEnabled) {
			packetOutOnly = packetOutOnlyEnabled;
			log.info("Configured. Packet-out only forwarding is {}",
					packetOutOnly ? "enabled" : "disabled");
		}
		boolean packetOutOfppTableEnabled = isPropertyEnabled(properties,
				"packetOutOfppTable");
		if (packetOutOfppTable != packetOutOfppTableEnabled) {
			packetOutOfppTable = packetOutOfppTableEnabled;
			log.info("Configured. Forwarding using OFPP_TABLE port is {}",
					packetOutOfppTable ? "enabled" : "disabled");
		}
		boolean ipv6ForwardingEnabled = isPropertyEnabled(properties,
				"ipv6Forwarding");
		if (ipv6Forwarding != ipv6ForwardingEnabled) {
			ipv6Forwarding = ipv6ForwardingEnabled;
			log.info("Configured. IPv6 forwarding is {}",
					ipv6Forwarding ? "enabled" : "disabled");
		}
		boolean matchDstMacOnlyEnabled = isPropertyEnabled(properties,
				"matchDstMacOnly");
		if (matchDstMacOnly != matchDstMacOnlyEnabled) {
			matchDstMacOnly = matchDstMacOnlyEnabled;
			log.info("Configured. Match Dst MAC Only is {}",
					matchDstMacOnly ? "enabled" : "disabled");
		}
		boolean matchVlanIdEnabled = isPropertyEnabled(properties,
				"matchVlanId");
		if (matchVlanId != matchVlanIdEnabled) {
			matchVlanId = matchVlanIdEnabled;
			log.info("Configured. Matching Vlan ID is {}",
					matchVlanId ? "enabled" : "disabled");
		}
		boolean matchIpv4AddressEnabled = isPropertyEnabled(properties,
				"matchIpv4Address");
		if (matchIpv4Address != matchIpv4AddressEnabled) {
			matchIpv4Address = matchIpv4AddressEnabled;
			log.info("Configured. Matching IPv4 Addresses is {}",
					matchIpv4Address ? "enabled" : "disabled");
		}
		boolean matchIpv4DscpEnabled = isPropertyEnabled(properties,
				"matchIpv4Dscp");
		if (matchIpv4Dscp != matchIpv4DscpEnabled) {
			matchIpv4Dscp = matchIpv4DscpEnabled;
			log.info("Configured. Matching IPv4 DSCP and ECN is {}",
					matchIpv4Dscp ? "enabled" : "disabled");
		}
		boolean matchIpv6AddressEnabled = isPropertyEnabled(properties,
				"matchIpv6Address");
		if (matchIpv6Address != matchIpv6AddressEnabled) {
			matchIpv6Address = matchIpv6AddressEnabled;
			log.info("Configured. Matching IPv6 Addresses is {}",
					matchIpv6Address ? "enabled" : "disabled");
		}
		boolean matchIpv6FlowLabelEnabled = isPropertyEnabled(properties,
				"matchIpv6FlowLabel");
		if (matchIpv6FlowLabel != matchIpv6FlowLabelEnabled) {
			matchIpv6FlowLabel = matchIpv6FlowLabelEnabled;
			log.info("Configured. Matching IPv6 FlowLabel is {}",
					matchIpv6FlowLabel ? "enabled" : "disabled");
		}
		boolean matchTcpUdpPortsEnabled = isPropertyEnabled(properties,
				"matchTcpUdpPorts");
		if (matchTcpUdpPorts != matchTcpUdpPortsEnabled) {
			matchTcpUdpPorts = matchTcpUdpPortsEnabled;
			log.info("Configured. Matching TCP/UDP fields is {}",
					matchTcpUdpPorts ? "enabled" : "disabled");
		}
		boolean matchIcmpFieldsEnabled = isPropertyEnabled(properties,
				"matchIcmpFields");
		if (matchIcmpFields != matchIcmpFieldsEnabled) {
			matchIcmpFields = matchIcmpFieldsEnabled;
			log.info("Configured. Matching ICMP (v4 and v6) fields is {}",
					matchIcmpFields ? "enabled" : "disabled");
		}
		Integer flowTimeoutConfigured = getIntegerProperty(properties,
				"flowTimeout");
		if (flowTimeoutConfigured == null) {
			log.info("Flow Timeout is not configured, default value is {}",
					flowTimeout);
		} else {
			flowTimeout = flowTimeoutConfigured;
			log.info("Configured. Flow Timeout is configured to {}",
					flowTimeout, " seconds");
		}
		Integer flowPriorityConfigured = getIntegerProperty(properties,
				"flowPriority");
		if (flowPriorityConfigured == null) {
			log.info("Flow Priority is not configured, default value is {}",
					flowPriority);
		} else {
			flowPriority = flowPriorityConfigured;
			log.info("Configured. Flow Priority is configured to {}",
					flowPriority);
		}
	}

	/**
	 * Get Integer property from the propertyName Return null if propertyName is
	 * not found.
	 *
	 * @param properties
	 *            properties to be looked up
	 * @param propertyName
	 *            the name of the property to look up
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

	/**
	 * Check property name is defined and set to true.
	 *
	 * @param properties
	 *            properties to be looked up
	 * @param propertyName
	 *            the name of the property to look up
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
	 * Packet processor responsible for forwarding packets along their paths.
	 */
	private class ReactivePacketProcessor implements PacketProcessor {

		@Override
		public void process(PacketContext context) {
			// Stop processing if the packet has been handled, since we
			// can't do any more to it.
			if (context.isHandled()) {
				return;
			}

			InboundPacket pkt = context.inPacket();

			OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);
			OFPacketIn.Builder pi = factory10.buildPacketIn();
			pi.setReason(OFPacketInReason.ACTION)
					.setData(pkt.unparsed().array())
					.setBufferId(getNewBufferId())
					.setXid(0)
					.setTotalLen(42)
					.setInPort(
							OFPort.of((int) pkt.receivedFrom().port().toLong()));

			savePacketIn(pi.build(), context);
			for (ClientController ctrl : clientCtrls) {
				NetIDEMessage msg = ctrl.EncodeOF10Message(pi.build(),
						deviceService.getDevice(pkt.receivedFrom().deviceId()));
				ctrl.getTcpClient().addMessage(msg);
			}
		}

	}

	private OFBufferId getNewBufferId() {
		return OFBufferId.of(BUFFER_ID++);
	}

	private void savePacketIn(OFPacketIn pi, PacketContext context) {
		inPackets.put(pi.getBufferId(), context);
	}

	private PacketContext getPacketContext(OFBufferId bufferId) {
		return inPackets.get(bufferId);
	}

	private void deletePacketIn(OFBufferId bufferId) {
		inPackets.remove(bufferId);
	}

	// Indicates whether this is a control packet, e.g. LLDP, BDDP
	private boolean isControlPacket(Ethernet eth) {
		short type = eth.getEtherType();
		return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
	}

	// Sends a packet out the specified port.
	private void packetOut(PacketContext context, PortNumber portNumber) {
		context.treatmentBuilder().setOutput(portNumber);
		context.send();
	}

	private OFFeaturesReply getFeatureReply(Device dev, List<Port> list) {
		OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);
		OFFeaturesReply.Builder fea = factory10.buildFeaturesReply();

		// log.info("device " + DatapathId.of(dev.id().toString()) + " " +
		// dev.hwVersion() + " " + dev.chassisId() + " " + dev.swVersion());
		// credo sia sbagliata
		fea.setDatapathId(DatapathId.of(dev.chassisId().value()));
		fea.setXid(0);
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
		Set<OFCapabilities> capabilities = new HashSet<OFCapabilities>();
		capabilities.add(OFCapabilities.FLOW_STATS);
		capabilities.add(OFCapabilities.PORT_STATS);
		capabilities.add(OFCapabilities.QUEUE_STATS);
		fea.setCapabilities(capabilities);
		fea.setNBuffers(4096);
		LinkedList<OFPortDesc> ports = new LinkedList<>();
		for (Port port : list) {
			OFPortDesc.Builder ofportdesc = factory10.buildPortDesc();
			ofportdesc.setPortNo(OFPort.of((int) port.number().toLong()));
			// ofportdesc.setCurrSpeed(port.portSpeed());
			ports.add(ofportdesc.build());
		}
		fea.setPorts(ports);
		return fea.build();
	}

	public void registerClientController(TCPClient tcpClient, NetIDEMessage msg) {
		ClientController clientCtrl = new ClientController(tcpClient, msg
				.getHello().getSupportedProtocols());
		clientCtrls.add(clientCtrl);
		tcpClient.addMessage(msg);
		for (Device dev : deviceService.getAvailableDevices()) {
			OFFeaturesReply fea = getFeatureReply(dev,
					deviceService.getPorts(dev.id()));
			if (clientCtrl.getPreferredProtocol() == NetIDEProtocol.OPENFLOW_1_0) {
				NetIDEMessage outMsg = clientCtrl.EncodeOF10Message(fea, dev);
				tcpClient.addMessage(outMsg);
			}
		}
	}

	public void processOFPacket(TCPClient tcpClient, NetIDEMessage msg) {
		ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
		buf.writeBytes(msg.getPayload());
		OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);
		OFMessageReader<OFMessage> reader = factory10.getReader();
		DeviceId deviceId = null;
		for (Device dev : deviceService.getAvailableDevices()) {
			if (dev.chassisId().value() == msg.getHeader().getDatapathId()) {
				deviceId = dev.id();
				break;
			}
		}
		try {
			OFMessage message = reader.readFrom(buf);
			if (message.getType().equals(OFType.FLOW_MOD)) {
				log.info("Flow mod");
				OFFlowMod fm = (OFFlowMod) message;
				log.info("Flow mod received: " + fm.toString());

				TrafficSelector selector = ShimTrafficSelector.buildSelector(fm
						.getMatch());
				TrafficTreatment treatment = ShimTrafficTreatment
						.buildActions(fm.getActions());

				ForwardingObjective forwardingObjective = DefaultForwardingObjective
						.builder().withSelector(selector)
						.withTreatment(treatment).withPriority(flowPriority)
						.withFlag(ForwardingObjective.Flag.VERSATILE)
						.fromApp(appId).makeTemporary(flowTimeout).add();

				flowObjectiveService.forward(deviceId, forwardingObjective);

			} else if (message.getType().equals(OFType.PACKET_OUT)) {
				log.info("Packet Out");
				OFPacketOut po = (OFPacketOut) message;
				log.info("Packet out received: " + po.toString());

				PortNumber portNumber = PortNumber.FLOOD;
				for (OFAction action : po.getActions()) {
					if (action.getType().equals(OFActionType.OUTPUT)) {
						portNumber = PortNumber
								.portNumber(((OFActionOutput) action).getPort()
										.getPortNumber());
						break;
					}
				}

				if (po.getBufferId() == OFBufferId.NO_BUFFER) {
					Ethernet ethPkt = new Ethernet();
					ethPkt.deserialize(po.getData(), 0, po.getData().length);
					log.info(ethPkt.toString());

					// Bail if this is deemed to be a control packet.
					if (isControlPacket(ethPkt)) {
						log.info(">>>>>>>> Pacchetto LLDP!!!!!!");
						manageLLDPPacket(ethPkt, po.getData(), deviceId,
								portNumber, tcpClient);

					} else
						log.error("NetIDE shim layer: packet out without buffer, drop it!");

				} else {

					PacketContext context = getPacketContext(po.getBufferId());
					packetOut(context, portNumber);
					deletePacketIn(po.getBufferId());
					log.info("Packet out mandato!");
				}
			} else
				log.error("Message not supported by NetIDE shim layer: "
						+ message.getType());

		} catch (OFParseError e) {
			log.error("OF Message not parsed by NetIDE shim layer: " + e);
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

	private void manageLLDPPacket(Ethernet ethPkt, byte[] data,
			DeviceId deviceId, PortNumber portNumber, TCPClient tcpClient) {
		
		ConnectPoint cp = new ConnectPoint(deviceId, portNumber);		
		Set<Link> links = linkService.getEgressLinks(cp);
		
		for (Link link : links) {
			OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);
			OFPacketIn.Builder pi = factory10.buildPacketIn();
			pi.setReason(OFPacketInReason.ACTION).setData(data)
					.setBufferId(OFBufferId.NO_BUFFER).setXid(0).setTotalLen(42)
					.setInPort(OFPort.of((int) link.dst().port().toLong()));
		
			for (ClientController ctrl : clientCtrls) {
				if (ctrl.getTcpClient().equals(tcpClient)) {
					NetIDEMessage msg = ctrl.EncodeOF10Message(pi.build(), 
							deviceService.getDevice(link.dst().deviceId()));
					tcpClient.addMessage(msg);
					log.info("LLDP Message sent back");
				}
			}
		}
	}
	
}
