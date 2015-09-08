package eu.netide.core.caos.execution;

import eu.netide.core.caos.resolution.ResolutionUtils;
import org.onlab.packet.*;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetTpDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetTpSrc;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.projectfloodlight.openflow.types.MacAddress;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by timvi on 08.09.2015.
 */
public class ExecutionUtils {
    public static Ethernet applyFlowMods(final List<OFFlowMod> flowMods, Ethernet original, OFPacketIn wrapperMessage) {
        OFFlowMod matchingFlowMod = getFirstMatchingFlowMod(flowMods, original, wrapperMessage);
        Ethernet modified = (Ethernet) original.clone();
        while (matchingFlowMod != null) {
            modified = (Ethernet) modified.clone();
            for (OFAction action : matchingFlowMod.getActions()) {
                if (action.getType() == OFActionType.SET_TP_SRC) {
                    ((TCP) modified.getPayload().getPayload()).setSourcePort((short) ((OFActionSetTpSrc) action).getTpPort().getPort());
                } else if (action.getType() == OFActionType.SET_TP_DST) {
                    ((TCP) modified.getPayload().getPayload()).setDestinationPort((short) ((OFActionSetTpDst) action).getTpPort().getPort());
                }
                // TODO support more actions
            }
            matchingFlowMod = getFirstMatchingFlowMod(flowMods, modified, wrapperMessage);
        }
        return modified;
    }

    public static OFFlowMod getFirstMatchingFlowMod(List<OFFlowMod> flowMods, Ethernet packet, OFPacketIn wrapperMessage) {
        Optional<OFFlowMod> result = flowMods.stream().filter(fm -> Arrays.stream(ResolutionUtils.MATCH_FIELDS_TO_CHECK).allMatch(field -> {
            try {
                return fm.getMatch().isFullyWildcarded(field) || getValue(packet, field, wrapperMessage).equals(fm.getMatch().get(field));
            } catch (Exception ex) {
                return false;
            }
        })).findFirst();
        return result.isPresent() ? result.get() : null;
    }

    public static OFValueType<?> getValue(Ethernet packet, MatchField field, OFPacketIn wrapperMessage) {
        try {
            if (field == MatchField.IN_PORT) {
                return wrapperMessage.getInPort();
            } else if (field == MatchField.ETH_TYPE) {
                return EthType.of(packet.getEtherType());
            } else if (field == MatchField.ETH_SRC) {
                return MacAddress.of(packet.getSourceMACAddress());
            } else if (field == MatchField.ETH_DST) {
                return MacAddress.of(packet.getDestinationMACAddress());
            } else if (field == MatchField.IP_PROTO) {
                return IpProtocol.of(((IPv4) packet.getPayload()).getProtocol());
            } else if (field == MatchField.IPV4_SRC) {
                return IPv4Address.of(((IPv4) packet.getPayload()).getSourceAddress());
            } else if (field == MatchField.IPV4_DST) {
                return IPv4Address.of(((IPv4) packet.getPayload()).getDestinationAddress());
            } else if (field == MatchField.IPV6_SRC) {
                return IPv6Address.of(((IPv6) packet.getPayload()).getSourceAddress());
            } else if (field == MatchField.IPV6_DST) {
                return IPv6Address.of(((IPv6) packet.getPayload()).getDestinationAddress());
            } else if (field == MatchField.TCP_SRC) {
                return TransportPort.of(((TCP) (packet.getPayload()).getPayload()).getSourcePort());
            } else if (field == MatchField.TCP_DST) {
                return TransportPort.of(((TCP) (packet.getPayload()).getPayload()).getDestinationPort());
            } else if (field == MatchField.UDP_SRC) {
                return TransportPort.of(((UDP) (packet.getPayload()).getPayload()).getSourcePort());
            } else if (field == MatchField.UDP_DST) {
                return TransportPort.of(((UDP) (packet.getPayload()).getPayload()).getDestinationPort());
            } else {
                throw new UnsupportedOperationException("Unknown field.");
            }
        } catch (Exception ex) {
            return TransportPort.NONE; // TODO check if this is a valid "matches nothing"
        }
    }
}
