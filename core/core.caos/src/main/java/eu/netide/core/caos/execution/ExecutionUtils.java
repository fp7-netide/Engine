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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Class containing utility methods for executors.
 *
 * Created by timvi on 08.09.2015.
 */
public class ExecutionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionUtils.class);

    /**
     * Applies all applicable FlowMods from a list of given FlowMods to the given Ethernet packet. The wrapperMessage is necessary for FlowMods that match on the InPort.
     *
     * @param flowMods       The list of flowmods to apply.
     * @param original       The packet to apply them to.
     * @param wrapperMessage The PakcetIn message that caused this. Needed for FlowMods that match on the InPort.
     * @return The modified packet.
     */
    public static Ethernet applyFlowMods(final List<OFFlowMod> flowMods, Ethernet original, OFPacketIn wrapperMessage) {
        OFFlowMod matchingFlowMod = getFirstMatchingFlowMod(flowMods, original, wrapperMessage);
        Ethernet modified = (Ethernet) original.clone();
        List<OFFlowMod> appliedFlowMods = new ArrayList<>();
        while (matchingFlowMod != null && !appliedFlowMods.contains(matchingFlowMod)) {
            modified = (Ethernet) modified.clone();
            for (OFAction action : matchingFlowMod.getActions()) {
                if (action.getType() == OFActionType.SET_TP_SRC) {
                    ((TCP) modified.getPayload().getPayload()).setSourcePort((short) ((OFActionSetTpSrc) action).getTpPort().getPort());
                } else if (action.getType() == OFActionType.SET_TP_DST) {
                    ((TCP) modified.getPayload().getPayload()).setDestinationPort((short) ((OFActionSetTpDst) action).getTpPort().getPort());
                }
                // TODO support more actions
            }
            appliedFlowMods.add(matchingFlowMod);
            matchingFlowMod = getFirstMatchingFlowMod(flowMods, modified, wrapperMessage);
        }
        return modified;
    }

    /**
     * Returns the applicable FlowMod with the highest priority from the given list of FlowMods.
     *
     * @param flowMods       The list of FlowMods to select from.
     * @param packet         The packet to match against.
     * @param wrapperMessage The original PacketIn message, required for matching against the InPort.
     * @return The matching FlowMod with the highest priority.
     */
    public static OFFlowMod getFirstMatchingFlowMod(List<OFFlowMod> flowMods, Ethernet packet, OFPacketIn wrapperMessage) {
        try {
            Comparator<OFFlowMod> comparator = (OFFlowMod first, OFFlowMod second) -> {
                try {
                    return Integer.compare(first.getPriority(), second.getPriority());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            };
            Optional<OFFlowMod> result = flowMods.stream()
                    .filter(fm -> doesMatch(fm, packet, wrapperMessage))
                    .sorted(comparator)
                    .findFirst();
            return result.isPresent() ? result.get() : null;
        } catch (Throwable ex) {
            logger.error("Error while finding matching flowmod.", ex);
            return null;
        }
    }

    private static boolean doesMatch(OFFlowMod flowMod, Ethernet packet, OFPacketIn wrapperMessage) {
        try {
            for (MatchField field : ResolutionUtils.MATCH_FIELDS_TO_CHECK) {
                if (!flowMod.getMatch().isFullyWildcarded(field) && !getValue(packet, field, wrapperMessage).equals(flowMod.getMatch().get(field)))
                    return false;
            }
            return true;
        } catch (Throwable ex) {
            logger.error("Exception while matching flowmod to packet.", ex);
            return false;
        }
    }

    /**
     * Gets an OFValue representation of the Matchfield from the given packet, effectively converting between IPacket and OpenFlowJ.
     *
     * @param packet         The packet to retrieve the values from.
     * @param field          The field to get.
     * @param wrapperMessage The original PacketIn message, necessary for the InPort value.
     * @return The OFValue representing the value of the given field in the given packet.
     */
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
        } catch (ClassCastException ex) {
            return TransportPort.NONE; // TODO check if this is a valid "matches nothing"
        }
    }
}
