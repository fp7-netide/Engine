package eu.netide.core.caos.execution;

import eu.netide.core.caos.composition.*;
import eu.netide.core.caos.composition.EthType;
import eu.netide.core.caos.composition.IpProtocol;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.types.*;

import static eu.netide.core.caos.composition.Condition.UNDEFINED_INT;
import static eu.netide.core.caos.composition.Condition.UNDEFINED_STRING;
import static eu.netide.core.caos.execution.ExecutionUtils.getValue;
import static org.projectfloodlight.openflow.protocol.match.MatchField.*;

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
        if (event != Events.PACKET_IN)
            throw new UnsupportedOperationException("Can only evaluate on packetIn messages.");
        // TODO support for other message types than PacketIn

        if (!condition.getEvents().isEmpty() && !condition.getEvents().stream().anyMatch(ce -> ce == event))
            return false;
        if (!condition.getDatapathIds().isEmpty() && !condition.getDatapathIds().stream().anyMatch(ce -> ce == status.getOriginalMessage().getHeader().getDatapathId()))
            return false;

        OpenFlowMessage currentMessage = (OpenFlowMessage) status.getCurrentMessage();
        OFPacketIn packetIn = (OFPacketIn) currentMessage.getOfMessage();

        Ethernet ethernet = (Ethernet) new Ethernet().deserialize(packetIn.getData(), 0, packetIn.getData().length);

        if (condition.getETH_TYPE() != EthType.UNDEFINED && condition.getETH_TYPE() != EthType.fromValue(ethernet.getEtherType()))
            return false;
        if (!condition.getETH_SRC().equals(UNDEFINED_STRING) && MacAddress.of(condition.getETH_SRC()).equals(getValue(ethernet, ETH_SRC, null)))
            return false;
        if (!condition.getETH_DST().equals(UNDEFINED_STRING) && MacAddress.of(condition.getETH_DST()).equals(getValue(ethernet, ETH_DST, null)))
            return false;
        if (condition.getIP_PROTO() != IpProtocol.HOPOPT && condition.getIP_PROTO().toOFJIpProtocol().equals(getValue(ethernet, IP_PROTO, null)))
            return false;
        if (condition.getIN_PORT() != UNDEFINED_INT && OFPort.of(condition.getIN_PORT()).equals(getValue(ethernet, IN_PORT, null)))
            return false;
        if (!condition.getIPV4_SRC().equals(UNDEFINED_STRING) && IPv4Address.of(condition.getIPV4_SRC()).equals(getValue(ethernet, IPV4_SRC, null)))
            return false;
        if (!condition.getIPV4_DST().equals(UNDEFINED_STRING) && IPv4Address.of(condition.getIPV4_DST()).equals(getValue(ethernet, IPV4_DST, null)))
            return false;
        if (!condition.getIPV6_SRC().equals(UNDEFINED_STRING) && IPv6Address.of(condition.getIPV6_SRC()).equals(getValue(ethernet, IPV6_SRC, null)))
            return false;
        if (!condition.getIPV4_DST().equals(UNDEFINED_STRING) && IPv6Address.of(condition.getIPV6_DST()).equals(getValue(ethernet, IPV6_DST, null)))
            return false;
        if (condition.getTCP_SRC() != UNDEFINED_INT && TransportPort.of(condition.getTCP_SRC()).equals(getValue(ethernet, TCP_SRC, null)))
            return false;
        if (condition.getTCP_DST() != UNDEFINED_INT && TransportPort.of(condition.getTCP_DST()).equals(getValue(ethernet, TCP_DST, null)))
            return false;
        if (condition.getUDP_SRC() != UNDEFINED_INT && TransportPort.of(condition.getUDP_SRC()).equals(getValue(ethernet, UDP_SRC, null)))
            return false;
        if (condition.getUDP_DST() != UNDEFINED_INT && TransportPort.of(condition.getUDP_DST()).equals(getValue(ethernet, UDP_DST, null)))
            return false;

        return true;
    }
}
