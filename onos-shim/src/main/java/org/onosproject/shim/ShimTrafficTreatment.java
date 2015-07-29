package org.onosproject.shim;

import java.util.List;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionCircuit;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;
import org.projectfloodlight.openflow.protocol.action.OFActionGroup;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionPopMpls;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetVlanPcp;
import org.projectfloodlight.openflow.protocol.action.OFActionSetVlanVid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOchSigidBasic;
import org.projectfloodlight.openflow.types.IPv4Address;

public class ShimTrafficTreatment {

	public static TrafficTreatment buildActions(List<OFAction> actions) {
		TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
		for (OFAction act : actions) {
			switch (act.getType()) {
			case OUTPUT:
				OFActionOutput out = (OFActionOutput) act;
				builder.setOutput(PortNumber.portNumber(out.getPort()
						.getPortNumber()));
				break;
			case SET_VLAN_VID:
				OFActionSetVlanVid vlan = (OFActionSetVlanVid) act;
				builder.setVlanId(VlanId.vlanId(vlan.getVlanVid().getVlan()));
				break;
			case SET_VLAN_PCP:
				OFActionSetVlanPcp pcp = (OFActionSetVlanPcp) act;
				builder.setVlanPcp(pcp.getVlanPcp().getValue());
				break;
			case SET_DL_DST:
				OFActionSetDlDst dldst = (OFActionSetDlDst) act;
				builder.setEthDst(MacAddress.valueOf(dldst.getDlAddr()
						.getLong()));
				break;
			case SET_DL_SRC:
				OFActionSetDlSrc dlsrc = (OFActionSetDlSrc) act;
				builder.setEthSrc(MacAddress.valueOf(dlsrc.getDlAddr()
						.getLong()));

				break;
			case SET_NW_DST:
				OFActionSetNwDst nwdst = (OFActionSetNwDst) act;
				IPv4Address di = nwdst.getNwAddr();
				builder.setIpDst(Ip4Address.valueOf(di.getInt()));
				break;
			case SET_NW_SRC:
				OFActionSetNwSrc nwsrc = (OFActionSetNwSrc) act;
				IPv4Address si = nwsrc.getNwAddr();
				builder.setIpSrc(Ip4Address.valueOf(si.getInt()));
				break;
			case POP_MPLS:
				OFActionPopMpls popMpls = (OFActionPopMpls) act;
				builder.popMpls((short) popMpls.getEthertype().getValue());
				break;
			case PUSH_MPLS:
				builder.pushMpls();
				break;
			case COPY_TTL_IN:
				builder.copyTtlIn();
				break;
			case COPY_TTL_OUT:
				builder.copyTtlOut();
				break;
			case DEC_MPLS_TTL:
				builder.decMplsTtl();
				break;
			case DEC_NW_TTL:
				builder.decNwTtl();
				break;
			case GROUP:
				OFActionGroup group = (OFActionGroup) act;
				builder.group(new DefaultGroupId(group.getGroup()
						.getGroupNumber()));
				break;
			case STRIP_VLAN:
			case POP_VLAN:
				builder.popVlan();
				break;
			case PUSH_VLAN:
				builder.pushVlan();
				break;
			case SET_TP_DST:
			case SET_TP_SRC:
			case POP_PBB:
			case PUSH_PBB:
			case SET_MPLS_LABEL:
			case SET_MPLS_TC:
			case SET_MPLS_TTL:
			case SET_NW_ECN:
			case SET_NW_TOS:
			case SET_NW_TTL:
			case SET_QUEUE:

			case ENQUEUE:
			default:
			}
		}
		return builder.build();
	}
}
