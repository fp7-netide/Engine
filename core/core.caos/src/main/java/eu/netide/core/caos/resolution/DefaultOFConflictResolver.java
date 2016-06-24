package eu.netide.core.caos.resolution;

import eu.netide.core.caos.composition.ResolutionPolicy;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.javatuples.Pair;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.projectfloodlight.openflow.protocol.OFType.FLOW_MOD;
import static org.projectfloodlight.openflow.protocol.OFType.PACKET_OUT;

/**
 * Default implementation of IConflictResolver for OpenFlow messages.
 * <p/>
 * Created by timvi on 24.08.2015.
 */
public class DefaultOFConflictResolver implements IConflictResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultOFConflictResolver.class);

    @Override
    public MessageType[] getSupportedMessageTypes() {
        return new MessageType[]{MessageType.OPENFLOW};
    }

    @Override
    public ResolutionPolicy[] getSupportedResolutionPolicies() {
        return new ResolutionPolicy[]{ResolutionPolicy.AUTO, ResolutionPolicy.PRIORITY, ResolutionPolicy.IGNORE};
    }

    @Override
    public ResolutionResult resolve(Message[] existingMessages, Message[] newMessages) {
        return resolve(existingMessages, newMessages, true);
    }

    @Override
    public ResolutionResult resolve(Message[] existingMessages, Message[] newMessages, boolean preferExisting) {
        PriorityInfo priorities = new PriorityInfo(0);
        for (Message message : preferExisting ? existingMessages : newMessages)
            priorities.addInfo(message.getHeader().getModuleId(), 1);
        return resolvePriority(Stream.concat(Arrays.stream(existingMessages), Arrays.stream(newMessages)).toArray(Message[]::new), priorities);
    }

    @Override
    public ResolutionResult resolve(Message[] messages, ResolutionPolicy policy, PriorityInfo priorities) {
        if (!containsConflict(messages)) {
            Dictionary<Message, ResolutionAction> actions = new Hashtable<>();
            for (Message m : messages)
                actions.put(m, ResolutionAction.NONE);
            return new ResolutionResult(messages, actions);
        }
        switch (policy) {
            case PASS:
                return resolvePass(messages);
            case AUTO:
                return resolveAuto(messages, priorities);
            case PRIORITY:
                return resolvePriority(messages, priorities);
            case IGNORE:
                return resolveIgnore(messages);
            default:
                throw new IllegalArgumentException("Unsupported policy type '" + policy.value() + "'.");
        }
    }

    private ResolutionResult resolvePass(Message[] messages) {
        Dictionary<Message, ResolutionAction> actions = new Hashtable<>();
        for (Message m : messages)
            actions.put(m, ResolutionAction.NONE);
        return new ResolutionResult(messages, actions);
    }

    @Override
    public boolean containsConflict(Message[] messages) {
        if (messages == null) {
            return false;
        }
        for (int i = 0; i < messages.length; i++) {
            for (int j = i + 1; j < messages.length; j++) {
                if (containsConflict(messages[i], messages[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsConflictDifferentPriorities(Message[] messages, PriorityInfo priorities) {
        if (messages == null) {
            return false;
        }
        for (int i = 0; i < messages.length; i++) {
            for (int j = i + 1; j < messages.length; j++) {
                if (containsConflict(messages[i], messages[j]) &&
                        (priorities.getPriority(messages[i].getHeader().getModuleId()) !=
                                priorities.getPriority(messages[j].getHeader().getModuleId()))
                        ) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public boolean containsConflict(Message message1, Message message2) {
        if (message1 == null || message2 == null) {
            return false;
        }
        if (message1.getHeader().getMessageType() != MessageType.OPENFLOW) {
            throw new IllegalArgumentException("Unsupported message type in first argument (" + message1.getHeader().getMessageType().name() + ").");
        }
        if (message2.getHeader().getMessageType() != MessageType.OPENFLOW) {
            throw new IllegalArgumentException("Unsupported message type in second argument (" + message2.getHeader().getMessageType().name() + ").");
        }
        OpenFlowMessage m1 = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(message1);
        OpenFlowMessage m2 = (OpenFlowMessage) NetIPUtils.ConcretizeMessage(message2);

        if (m1.getOfMessage().getType() == FLOW_MOD && m2.getOfMessage().getType() == FLOW_MOD) {
            Stream<OFMatchConflict> result = ResolutionUtils.getMatchConflicts(m1, m2, fM(m1).getMatch(), fM(m2).getMatch());
            return result.count() != 0;
        }

        // We let two Packet Out always conflict
        if (m1.getOfMessage().getType() == PACKET_OUT && m2.getOfMessage().getType() == PACKET_OUT)
            return true;

        // TODO other OF message types (?)

        return false;
    }

    /**
     * Resolves the given messages with the AUTO policy.
     *
     * @param messages   The messages.
     * @param priorities [Optional] Information about module priorities.
     * @return The result of the resolution.
     */
    private ResolutionResult resolveAuto(Message[] messages, PriorityInfo priorities) {
        RuleWorkingSet workingSet = new RuleWorkingSet(Arrays.stream(messages).map(m -> (OpenFlowMessage) NetIPUtils.ConcretizeMessage(m)));

        // If the messages are not flow mods, we cannot do anything
        if (!workingSet.getMessages().allMatch(m -> m.getOfMessage().getType() == FLOW_MOD)) {
            throw new UnsupportedOperationException("Can only automatically merge FLOW_MOD messages.");
        }

        Dictionary<Message, ResolutionAction> actions = new Hashtable<>();
        boolean hasOptimized = true;
        List<Pair<OpenFlowMessage, OpenFlowMessage>> processedPairs = new ArrayList<>();
        int i = 0;
        while (hasOptimized) {
            if (i % 100 == 0) {
                log.info("Resolving in round " + i);
            }
            hasOptimized = false;
            RuleWorkingSet newWorkingSet = new RuleWorkingSet();
            for (Pair<OpenFlowMessage, OpenFlowMessage> pair : workingSet.getPairs().collect(Collectors.toList())) {
                OpenFlowMessage m1 = pair.getValue0();
                OpenFlowMessage m2 = pair.getValue1();

                // if this pair was already processed, skip it
                if (processedPairs.stream().anyMatch(p -> p.getValue0().equals(m1) && p.getValue1().equals(m2))) {
                    continue;
                }

                List<OFMatchConflict> conflicts = ResolutionUtils.getMatchConflicts(m1, m2, fM(m1).getMatch(), fM(m2).getMatch()).collect(Collectors.toList());
                if (conflicts.size() == 0) {
                    // no conflicts for these two messages -> take both to the next round
                    workingSet.getMessages().forEach(newWorkingSet::addDistinct);
                    actions.put(m1, ResolutionAction.NONE);
                    actions.put(m2, ResolutionAction.NONE);
                    processedPairs.add(pair);
                    hasOptimized = true;
                    break;
                } else {
                    // Generate a candidate combination flowmod
                    OFFlowMod candidate = ResolutionUtils.generateCandidate(fM(m1), fM(m2));
                    OpenFlowMessage candidateMessage = ResolutionUtils.messageFromFlowMod(candidate);

                    // Check candidate applicability
                    if (ResolutionUtils.areEquivalentMatches(fM(m1).getMatch(), fM(m2).getMatch())) {
                        // m1 == m2 -> candidate represents combination, can replace both
                        actions.put(m1, ResolutionAction.REPLACED_AUTO);
                        actions.put(m2, ResolutionAction.REPLACED_AUTO);
                        actions.put(candidateMessage, ResolutionAction.CREATED_AUTO_REPLACEMENT);
                        log.info("Introduced new replacement rule for messages '" + m1.toString() + "' and '" + m2.toString() + "'.");
                        newWorkingSet.addDistinct(candidateMessage);
                        workingSet.getMessages().forEach(m -> {
                            if (!m.equals(m1) && !m.equals(m2)) {
                                newWorkingSet.addDistinct(m);
                            }
                        });
                        hasOptimized = true;
                        break;
                    } else if (ResolutionUtils.areEquivalent(candidate, fM(m1)) || ResolutionUtils.areEquivalent(candidate, fM(m2))) {// check whether candidate is equivalent to existing rule
                        // don't introduce candidate, keep m1,m2
                        actions.put(m1, ResolutionAction.NONE);
                        actions.put(m2, ResolutionAction.NONE);
                        workingSet.getMessages().forEach(newWorkingSet::addDistinct);
                        processedPairs.add(pair);
                        hasOptimized = true;
                        break;
                    } else {
                        newWorkingSet.addDistinct(candidateMessage);
                        actions.put(candidateMessage, ResolutionAction.CREATED_AUTO_COMBINATION);
                        workingSet.getMessages().forEach(newWorkingSet::addDistinct);
                        log.info("Introduced new combination rule for messages '" + m1.toString() + "' and '" + m2.toString() + "'.");
                        processedPairs.add(pair);
                        hasOptimized = true;
                        break;
                    }
                }
            }
            if (hasOptimized) {
                workingSet = newWorkingSet;
            }
        }
        return new ResolutionResult(workingSet.toMessageArray(), actions);
    }

    /**
     * Returns the casted OFFlowMod component of the OpenFlowMessage.
     *
     * @param message The OpenFlowMessage.
     * @return The OFFlowMod contained in the message.
     */
    private OFFlowMod fM(OpenFlowMessage message) {
        return (OFFlowMod) message.getOfMessage();
    }

    /**
     * Resolves the given messages with the PRIORITY policy.
     *
     * @param messages   The messages.
     * @param priorities Information about module priorities.
     * @return The result of the resolution.
     */
    private ResolutionResult resolvePriority(Message[] messages, PriorityInfo priorities) {
        Dictionary<Message, ResolutionAction> actions = new Hashtable<>();
        for (Message m : messages)
            actions.put(m, ResolutionAction.NONE);
        while (containsConflictDifferentPriorities(messages, priorities)) {
            for (int i = 0; i < messages.length; i++) {
                for (int j = i + 1; j < messages.length; j++) {
                    if (containsConflict(messages[i], messages[j])) {
                        int p1 = priorities.getPriority(messages[i].getHeader().getModuleId());
                        int p2 = priorities.getPriority(messages[j].getHeader().getModuleId());
                        if (p1 == p2) {
                            actions.put(messages[i], ResolutionAction.NONE);
                            actions.put(messages[i], ResolutionAction.NONE);
                        } else if (p1 > p2) {
                            actions.put(messages[i], ResolutionAction.NONE);
                            actions.put(messages[j], ResolutionAction.IGNORED);
                            messages[j] = null;
                        } else if (p2 > p1) {
                            actions.put(messages[i], ResolutionAction.IGNORED);
                            actions.put(messages[j], ResolutionAction.NONE);
                            messages[i] = null;
                        } else {
                            actions.put(messages[i], ResolutionAction.IGNORED);
                            actions.put(messages[j], ResolutionAction.IGNORED);
                            messages[i] = null;
                            messages[j] = null;
                        }
                    }
                }
            }
        }
        return new ResolutionResult(Arrays.stream(messages).filter(m -> m != null).toArray(Message[]::new), actions);
    }

    /**
     * Resolves the given messages with the IGNORE policy.
     *
     * @param messages The messages.
     * @return The result of the resolution.
     */
    private ResolutionResult resolveIgnore(Message[] messages) {
        Dictionary<Message, ResolutionAction> actions = new Hashtable<>();
        for (Message m : messages)
            actions.put(m, ResolutionAction.NONE);
        while (containsConflict(messages)) {
            for (int i = 0; i < messages.length; i++) {
                for (int j = i + 1; j < messages.length; j++) {
                    if (containsConflict(messages[i], messages[j])) {
                        actions.put(messages[i], ResolutionAction.IGNORED);
                        actions.put(messages[j], ResolutionAction.IGNORED);
                        messages[i] = null; // "delete" from list
                        messages[j] = null;
                    }
                }
            }
        }
        return new ResolutionResult(Arrays.stream(messages).filter(m -> m != null).toArray(Message[]::new), actions);
    }
}
