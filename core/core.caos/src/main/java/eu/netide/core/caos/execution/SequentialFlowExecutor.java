package eu.netide.core.caos.execution;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.caos.composition.ExecutionFlowNode;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.ResolutionPolicy;
import eu.netide.core.caos.resolution.ConflictResolvers;
import eu.netide.core.caos.resolution.IConflictResolver;
import eu.netide.core.caos.resolution.ResolutionResult;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by timvi on 31.08.2015.
 */
public class SequentialFlowExecutor implements IFlowExecutor {

    private static final Logger log = LoggerFactory.getLogger(SequentialFlowExecutor.class);

    @Override
    public ExecutionFlowStatus executeFlow(ExecutionFlowStatus status, Stream<ExecutionFlowNode> nodes, IShimManager shimManager, IBackendManager backendManager) {
        if (status.getOriginalMessage().getHeader().getMessageType() != MessageType.OPENFLOW) {
            throw new UnsupportedOperationException("Can only handle flows initiated by an OpenFlow message.");
        }

        OpenFlowMessage originalMessage = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(status.getOriginalMessage());

        if (originalMessage.getOfMessage().getType() != OFType.PACKET_IN) {
            throw new UnsupportedOperationException("Can only handle flows initiated by an OpenFlow PacketIn message.");
        }

        OFPacketIn originalPacketIn = (OFPacketIn) originalMessage.getOfMessage();
        List<ExecutionFlowNode> collectedNodes = nodes.collect(Collectors.toList());
        ExecutionFlowNode executionFlowNode = collectedNodes.get(0);

        // request results
        status = FlowNodeExecutors.getExecutor(executionFlowNode).execute(executionFlowNode, status, shimManager, backendManager);

        // prepare message(s) for next node, if there is one
        if (collectedNodes.size() > 1) {
            // Do network emulation
            Ethernet ethernet = (Ethernet) new Ethernet().deserialize(originalPacketIn.getData(), 0, originalPacketIn.getData().length);
            IPacket[] newPackets = ExecutionUtils.emulateNetworkBehaviour(status.getResultMessages(), ethernet, originalPacketIn);


            if (newPackets.length == 1) {
                // no multiflow (i.e. more than one new packet)
                return this.executeFlow(status, collectedNodes.stream().skip(1), shimManager, backendManager);
            }
            // MultiFlow
            ExecutionFlowStatus[] statuses = new ExecutionFlowStatus[newPackets.length];
            int i = 0;
            for (IPacket packet : newPackets) {
                OFPacketIn newPacketIn = originalPacketIn.createBuilder().setData(packet.serialize()).build();
                OpenFlowMessage newMessage = new OpenFlowMessage();
                newMessage.setOfMessage(newPacketIn);
                statuses[i] = status.clone(newMessage);
                i++;
            }
            return this.executeMultiFlow(status, statuses, collectedNodes.stream().skip(1), shimManager, backendManager);
        } else {
            log.info("End of sequential flow reached. Collected " + status.getResultMessages().size() + " messages.");
            return status;
        }
    }

    private ExecutionFlowStatus executeMultiFlow(ExecutionFlowStatus status, ExecutionFlowStatus[] clonedStatuses, Stream<ExecutionFlowNode> nodes, IShimManager shimManager, IBackendManager backendManager) {
        List<ExecutionFlowNode> collectedNodes = nodes.collect(Collectors.toList());
        List<ExecutionFlowStatus> resultStatuses = new ArrayList<>();
        for (ExecutionFlowStatus efs : clonedStatuses) {
            resultStatuses.add(this.executeFlow(efs, collectedNodes.stream(), shimManager, backendManager));
        }
        // merge all into existing status
        Map<Long, List<Message>> multiFlowMessagesByDatapath = resultStatuses.stream().flatMap(efs -> efs.getResultMessages().values().stream().flatMap(Collection::stream)).collect(Collectors.toSet()).stream().collect(Collectors.groupingBy(m -> m.getHeader().getDatapathId()));

        for (Long datapathId : multiFlowMessagesByDatapath.keySet()) {
            if (!status.getResultMessages().containsKey(datapathId)) {
                // no existing rules for that switch -> accept all new ones and continue
                status.getResultMessages().put(datapathId, multiFlowMessagesByDatapath.get(datapathId));
                log.info("Adding new rule for unused switch '" + datapathId + "': " + multiFlowMessagesByDatapath.get(datapathId).toString());
                continue;
            }
            // resolve per switch
            Message[] newMessages = multiFlowMessagesByDatapath.get(datapathId).stream().toArray(Message[]::new);
            IConflictResolver resolver = ConflictResolvers.getMatchingResolver(newMessages);
            log.info("Using resolver " + resolver.getClass().getName());
            // first resolve within the set (use AUTO for now)
            // TODO make configurable
            ResolutionResult rr = resolver.resolve(newMessages, ResolutionPolicy.AUTO, null);
            // merge set into existing status
            rr = resolver.resolve(status.getResultMessages().get(datapathId).stream().toArray(Message[]::new), rr.getResultingMessagesToSend(), true);
            // TODO setting for preferExisting
            status.getResultMessages().put(datapathId, Arrays.asList(rr.getResultingMessagesToSend()));
        }
        log.info("Collected " + status.getResultMessages().size() + " messages from multiflow.");
        return status;
    }
}
