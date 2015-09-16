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
        log.info("Starting sequential execution for original packetIn: " + originalPacketIn.toString() + ".");
        status.setCurrentMessage(originalMessage);
        int i = 0;
        int skipped = 0;
        for (ExecutionFlowNode efn : nodes) {
            // request results
            ExecutionResult result = FlowNodeExecutors.getExecutor(efn).execute(efn, status, backendManager);

            if (result.equals(ExecutionResult.SKIPPED)) {
                skipped++;
                log.info("Skipping node: " + efn.toString());
                continue;
            }

            // merge results
            Message[] newMessages = result.getMessagesToSend().toArray(Message[]::new);
            log.info("Got " + newMessages.length + " messages back for node " + efn.toString());
            IConflictResolver resolver = ConflictResolvers.getMatchingResolver(newMessages);
            log.info("Using resolver " + resolver.getClass().getName());
            ResolutionResult rr = resolver.resolve(status.getResultMessages().stream().toArray(Message[]::new), newMessages, true);
            // TODO setting for preferExisting
            status.setResultMessages(Arrays.asList(rr.getResultingMessagesToSend()));
            log.info("Conflicts resolved, now " + status.getResultMessages().size() + " messages in total.");
            // prepare message for next node
            List<OFFlowMod> flowModsUpToNow = status.getResultMessages().stream().map(rm -> ((OFFlowMod) ((OpenFlowMessage) NetIPUtils.ConcretizeMessage(rm)).getOfMessage())).collect(Collectors.toList());
            log.info("Collected " + flowModsUpToNow.size() + " flowmods.");
            Ethernet ethernet = (Ethernet) new Ethernet().deserialize(originalPacketIn.getData(), 0, originalPacketIn.getData().length);
            log.info("Ethernet packet deserialized.");
            IPacket newPacket = ExecutionUtils.applyFlowMods(flowModsUpToNow, ethernet, originalPacketIn);
            log.info("New packet generated.");
            OFPacketIn newPacketIn = originalPacketIn.createBuilder().setData(newPacket.serialize()).build();
            OpenFlowMessage newMessage = new OpenFlowMessage();
            newMessage.setOfMessage(newPacketIn);
            log.info("New message created.");
            status.setCurrentMessage(newMessage);
            i++;
            log.info("Finished execution for node " + efn.toString());
        }
        log.info("Sequential execution finished for " + i + " nodes (" + skipped + " skipped).");
        return status;
    }
}
