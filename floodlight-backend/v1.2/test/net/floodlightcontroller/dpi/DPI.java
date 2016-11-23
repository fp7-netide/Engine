package net.floodlightcontroller.dpi;

import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.packet.Ethernet;

public class DPI implements IFloodlightModule, IOFMessageListener {
	protected static Logger log = LoggerFactory.getLogger(DPI.class);
	protected IFloodlightProviderService floodlightProviderService;
	protected IDebugCounterService debugCounterService;
	private MacAddress hostMac;
	
	@Override
	public String getName() {
		return "DPI";
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		if (msg.getType().equals(OFType.PACKET_IN)){
			OFPacketIn pi = (OFPacketIn) msg;
			OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
			Match m = createMatchFromPacket(sw, inPort, cntx);
			MacAddress sourceMac = m.get(MatchField.ETH_SRC);
			MacAddress destMac = m.get(MatchField.ETH_DST);
			if (sourceMac != null && sourceMac.equals(hostMac)){
				log.info("PacketIN message filter on source MAC");
				printPacketIN(pi);
			}
			
			if (destMac != null && destMac.equals(hostMac)){
				log.info("PacketIN message filter on destination MAC");
				printPacketIN(pi);
			}
		}
		return Command.CONTINUE;
	}
	
	private void printPacketIN(OFPacketIn pi){
		log.info("XID:" + pi.getXid() + " OFVersion: " + pi.getVersion());
		log.info("BufferID: " + pi.getBufferId() + " TotalLen: " + pi.getTotalLen() + " InPort: " + pi.getInPort());
		log.info("Reason: " + pi.getReason());
		log.info("Data: " + pi.getData());
	}
	
	protected Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx) {
		// The packet in match will only contain the port number.
		// We need to add in specifics for the hosts we're routing between.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();

		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, inPort)
		.setExact(MatchField.ETH_SRC, srcMac)
		.setExact(MatchField.ETH_DST, dstMac);

		if (!vlan.equals(VlanVid.ZERO)) {
			mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
		}

		return mb.build();
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		debugCounterService = context.getServiceImpl(IDebugCounterService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);
		String hostMacString = context.getConfigParams(this).getOrDefault("hostMAC", "FF:FF:FF:FF:FF:FF");
		log.info("Filtering PacketIN on MAC address: " + hostMacString);
		hostMac = MacAddress.of(hostMacString);
	}

}
