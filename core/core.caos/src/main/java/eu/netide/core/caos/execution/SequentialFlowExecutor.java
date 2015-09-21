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
    public ExecutionFlowStatus executeFlow(ExecutionFlowStatus status, Stream<ExecutionFlowNode> nodes, IBackendManager backendManager) {
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
        ExecutionResult result = FlowNodeExecutors.getExecutor(executionFlowNode).execute(executionFlowNode, status, backendManager);

        if (result.equals(ExecutionResult.SKIPPED)) {
            log.info("Skipping node: " + executionFlowNode.toString());
            // continue with the rest recursively (if existent)
            if (collectedNodes.size() > 1) {
                log.info("Finished execution for node " + executionFlowNode.toString());
                return this.executeFlow(status, collectedNodes.stream().skip(1), backendManager);
            } else {
                handleFlowEnd(status, backendManager);
                return status;
            }
        }

        // merge results per datapath
        log.info("Got " + result.getMessagesToSend().count() + " messages back for node " + executionFlowNode.toString());
        Map<Long, List<Message>> newMessagesByDatapath = result.getMessagesToSend().collect(Collectors.groupingBy(message -> message.getHeader().getDatapathId()));

        for (Long datapathId : newMessagesByDatapath.keySet()) {
            if (!status.getResultMessages().containsKey(datapathId)) {
                // no existing rules for that switch -> accept all new ones and continue
                status.getResultMessages().put(datapathId, newMessagesByDatapath.get(datapathId));
                log.info("Adding new rule for unused switch '" + datapathId + "': " + newMessagesByDatapath.get(datapathId).toString());
                continue;
            }
            // resolve per switch
            Message[] newMessages = newMessagesByDatapath.get(datapathId).stream().toArray(Message[]::new);
            IConflictResolver resolver = ConflictResolvers.getMatchingResolver(newMessages);
            log.info("Using resolver " + resolver.getClass().getName());
            ResolutionResult rr = resolver.resolve(status.getResultMessages().get(datapathId).stream().toArray(Message[]::new), newMessages, true);
            // TODO setting for preferExisting
            status.getResultMessages().put(datapathId, Arrays.asList(rr.getResultingMessagesToSend()));
        }

        // prepare message(s) for next node, if there is one
        if (collectedNodes.size() > 1) {
            Ethernet ethernet = (Ethernet) new Ethernet().deserialize(originalPacketIn.getData(), 0, originalPacketIn.getData().length);
            IPacket[] newPackets = ExecutionUtils.emulateNetworkBehaviour(status.getResultMessages(), ethernet, originalPacketIn);
            log.info("New packets generated.");
            for (IPacket packet : newPackets) { // TODO do this in parallel?
                // continue execution for each packet
                OFPacketIn newPacketIn = originalPacketIn.createBuilder().setData(newPackets[0].serialize()).build();
                OpenFlowMessage newMessage = new OpenFlowMessage();
                newMessage.setOfMessage(newPacketIn);
                // create status for this execution branch
                ExecutionFlowStatus clonedStatus = new ExecutionFlowStatus(newMessage);
                Map<Long, List<Message>> clonedMap = new HashMap<>();
                for (Long dpid : status.getResultMessages().keySet()) {
                    clonedMap.put(dpid, (List<Message>) ((ArrayList<Message>) status.getResultMessages().get(dpid)).clone());
                }
                clonedStatus.setResultMessages(clonedMap);
                // trigger execution
                log.info("Finished execution for node " + executionFlowNode.toString());
                this.executeFlow(clonedStatus, collectedNodes.stream().skip(1), backendManager);
            }
            return null; // TODO handle this case properly
        } else {
            handleFlowEnd(status, backendManager);
            return status;
        }
    }

    private void handleFlowEnd(ExecutionFlowStatus endStatus, IBackendManager backendManager) {
        log.info("End of sequential flow reached. Sending to " + endStatus.getResultMessages().size() + " different switches.");
        for (Map.Entry<Long, List<Message>> entry : endStatus.getResultMessages().entrySet()) {
            entry.getValue().stream().forEach(backendManager::sendMessage);
            log.info("Sent " + entry.getValue().size() + " rules to switch " + entry.getKey());
        }
    }
}
