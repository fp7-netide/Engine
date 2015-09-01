package eu.netide.core.caos.execution;

import eu.netide.core.caos.composition.Condition;
import eu.netide.core.caos.composition.Events;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;

/**
 * Class for evaluating conditions.
 * <p>
 * Created by timvi on 31.08.2015.
 */
public class ConditionEvaluator {
    
    /**
     * Evaluates the given condition in the context of the given ExecutionFlowStatus (original message).
     *
     * @param condition The condition to evaluate.
     * @param status    The ExecutionFlowStatus to take the original message information from.
     * @return True, if the condition holds. False otherwise.
     */
    public static boolean evaluate(Condition condition, ExecutionFlowStatus status) {
        Message concreteMessage = NetIPUtils.ConcretizeMessage(status.getOriginalMessage());
        if (!(concreteMessage instanceof OpenFlowMessage))
            throw new UnsupportedOperationException("Can only evaluate for OpenFlow messages");

        OpenFlowMessage openFlowMessage = (OpenFlowMessage) concreteMessage;

        Events event = Events.fromValue(openFlowMessage.getOfMessage().getType().name());
        Match match;
        if (!condition.getEvents().stream().anyMatch(ce -> ce == event))
            return false;
        switch (event) {
            case PACKET_IN:
                match = ((OFPacketIn) openFlowMessage.getOfMessage()).getMatch();
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized event type '" + openFlowMessage.getOfMessage().getType().name());
        }


        if (!match.get(MatchField.IPV4_SRC).equals(IPv4Address.of(condition.getIPV4_SRC())))
            return false;
        if (!match.get(MatchField.IPV4_DST).equals(IPv4Address.of(condition.getIPV4_DST())))
            return false;
        if (!match.get(MatchField.IPV6_SRC).equals(IPv6Address.of(condition.getIPV6_SRC())))
            return false;
        if (!match.get(MatchField.IPV6_DST).equals(IPv6Address.of(condition.getIPV6_DST())))
            return false;
        if (!match.get(MatchField.ETH_SRC).equals(MacAddress.of(condition.getETH_SRC())))
            return false;
        if (!match.get(MatchField.ETH_DST).equals(MacAddress.of(condition.getETH_DST())))
            return false;
        if (!match.get(MatchField.ETH_TYPE).equals(condition.getETH_TYPE().toOFJEthType()))
            return false;
        //if (!match.get(MatchField.IN_PORT).equals(condition.getIN_PORT())) // TODO handle OFJ ports
        //  return false;
        if (!match.get(MatchField.TCP_SRC).equals(TransportPort.of(condition.getTCP_SRC())))
            return false;
        if (!match.get(MatchField.TCP_DST).equals(TransportPort.of(condition.getTCP_DST())))
            return false;
        if (!match.get(MatchField.UDP_SRC).equals(TransportPort.of(condition.getUDP_SRC())))
            return false;
        if (!match.get(MatchField.UDP_DST).equals(TransportPort.of(condition.getUDP_DST())))
            return false;

        return true;
    }
}
