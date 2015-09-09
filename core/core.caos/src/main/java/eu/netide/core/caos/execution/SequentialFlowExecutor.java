package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ExecutionResult;
import eu.netide.core.caos.resolution.ConflictResolvers;
import eu.netide.core.caos.resolution.IConflictResolver;
import eu.netide.core.caos.resolution.ResolutionResult;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by timvi on 31.08.2015.
 */
public class SequentialFlowExecutor implements IFlowExecutor {

    private static final Logger log = LoggerFactory.getLogger(SequentialFlowExecutor.class);

    @Override
    public ExecutionFlowStatus executeFlow(ExecutionFlowStatus status, Iterable<ExecutionFlowNode> nodes, IBackendManager backendManager) {
        if (status.getOriginalMessage().getHeader().getMessageType() != MessageType.OPENFLOW) {
            throw new UnsupportedOperationException("Can only handle flows initiated by an OpenFlow message.");
        }
        OpenFlowMessage originalMessage = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(status.getOriginalMessage());
        if (originalMessage.getOfMessage().getType() != OFType.PACKET_IN) {
            throw new UnsupportedOperationException("Can only handle flows initiated by an OpenFlow PacketIn message.");
        }
        OFPacketIn originalPacketIn = (OFPacketIn) originalMessage.getOfMessage();
        log.debug("Starting sequential execution for original packetIn: " + originalPacketIn.toString() + ".");
        status.setCurrentMessage(originalMessage);
        int i = 0;
        int skipped = 0;
        for (ExecutionFlowNode efn : nodes) {
            // request results
            ExecutionResult result = FlowNodeExecutors.getExecutor(efn).execute(efn, status, backendManager);

            if (result.equals(ExecutionResult.SKIPPED)) {
                skipped++;
                continue;
            }

            // merge results
            Message[] newMessages = result.getMessagesToSend().toArray(Message[]::new);
            IConflictResolver resolver = ConflictResolvers.getMatchingResolver(newMessages);
            ResolutionResult rr = resolver.resolve(status.getResultMessages().stream().toArray(Message[]::new), newMessages, true);
            // TODO setting for preferExisting
            status.setResultMessages(Arrays.asList(rr.getResultingMessagesToSend()));
            // prepare message for next node
            List<OFFlowMod> flowModsUpToNow = status.getResultMessages().stream().map(rm -> ((OFFlowMod) ((OpenFlowMessage) NetIPUtils.ConcretizeMessage(status.getCurrentMessage())).getOfMessage())).collect(Collectors.toList());
            IPacket newPacket = ExecutionUtils.applyFlowMods(flowModsUpToNow, (Ethernet) new Ethernet().deserialize(originalPacketIn.getData(), 0, originalPacketIn.getData().length), originalPacketIn);
            OFPacketIn newPacketIn = originalPacketIn.createBuilder().setData(newPacket.serialize()).build();
            OpenFlowMessage newMessage = new OpenFlowMessage();
            newMessage.setOfMessage(newPacketIn);
            status.setCurrentMessage(newMessage);
            i++;
        }
        log.debug("Sequential execution finished for " + i + " nodes (" + skipped + " skipped).");
        return status;
    }
}
