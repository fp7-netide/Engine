package eu.netide.core.caos.resolution;

import eu.netide.core.caos.composition.ResolutionPolicy;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.javatuples.Pair;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.netide.core.caos.resolution.OFMatchConflict.Type.Same;
import static org.projectfloodlight.openflow.protocol.OFType.FLOW_MOD;

/**
 * Default implementation of IConflictResolver for OpenFlow messages.
 * <p>
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
        for (Message message : existingMessages)
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

    @Override
    public boolean containsConflict(Message[] messages) {
        if (messages == null) return false;
        for (int i = 0; i < messages.length; i++) {
            for (int j = i + 1; j < messages.length; j++) {
                if (containsConflict(messages[i], messages[j])) return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsConflict(Message message1, Message message2) {
        if (message1 == null || message2 == null) return false;
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
        Dictionary<Message, ResolutionAction> actions = new Hashtable<>();
        RuleWorkingSet workingSet = new RuleWorkingSet(Arrays.stream(messages).map(m -> (OpenFlowMessage) NetIPUtils.ConcretizeMessage(m)));
        boolean hasOptimized = true;
        while (hasOptimized) {
            hasOptimized = false;
            RuleWorkingSet newWorkingSet = new RuleWorkingSet();
            for (Pair<OpenFlowMessage, OpenFlowMessage> pair : workingSet.getPairs().collect(Collectors.toList())) {
                OpenFlowMessage m1 = pair.getValue0();
                OpenFlowMessage m2 = pair.getValue1();

                if (m1.getOfMessage().getType() != FLOW_MOD || m2.getOfMessage().getType() != FLOW_MOD) {
                    throw new UnsupportedOperationException("Can only automatically merge FLOW_MOD messages. Found '" + m1.getOfMessage().getType().name() + "' and '" + m2.getOfMessage().getType().name() + "'.");
                }

                Stream<OFMatchConflict> conflicts = ResolutionUtils.getMatchConflicts(m1, m2, fM(m1).getMatch(), fM(m2).getMatch());
                if (conflicts.count() == 0) {
                    // no conflicts for these two messages -> take both to the next round
                    newWorkingSet.addDistinct(m1);
                    newWorkingSet.addDistinct(m2);
                    actions.put(m1, ResolutionAction.NONE);
                    actions.put(m2, ResolutionAction.NONE);
                } else {
                    // there are conflicts -> merge both messages into one (replacing or combining)
                    // when all match fields are equal, we replace the two rules by a new one that has all actions.
                    // Otherwise, we just introduce a new rule for the combined case with all actions.
                    boolean replacing = conflicts.allMatch(c -> c.getType() == Same);
                    Match.Builder matchBuilder = fM(m1).getMatch().createBuilder(); // start from m1 clone
                    conflicts.forEach(c -> {
                        // replace each conflicting field with the value of the exact message (message2)
                        matchBuilder.setExact(c.getMatchField(), fM((OpenFlowMessage) c.getMessage2()).getMatch().get(c.getMatchField()));
                    });
                    // create new flowmod
                    OFFlowMod flowMod = OFFactories.getFactory(OFVersion.OF_10).buildFlowModify()
                            .setActions(Stream.concat(fM(m1).getActions().stream(), fM(m2).getActions().stream()).collect(Collectors.toList()))
                            .setMatch(fM(m1).getMatch())
                            .setPriority(Math.max(fM(m1).getPriority(), fM(m2).getPriority()) + 1).build();
                    // TODO calculate priority, set remaining important fields (?), check OF version
                    OpenFlowMessage newMessage = messageFromFlowMod(flowMod);
                    // add to ruleset
                    boolean added = newWorkingSet.addDistinctBasedOnMatch(newMessage);
                    if (added) {
                        log.debug("Introduced new " + (replacing ? "replacement" : "combination") + " rule for two flowMods");
                        actions.put(newMessage, replacing ? ResolutionAction.CREATED_AUTO_REPLACEMENT : ResolutionAction.CREATED_AUTO_COMBINATION);
                        actions.put(m1, replacing ? ResolutionAction.REPLACED_AUTO : ResolutionAction.NONE);
                        actions.put(m2, replacing ? ResolutionAction.REPLACED_AUTO : ResolutionAction.NONE);
                        hasOptimized = true;
                        if (!replacing) {
                            newWorkingSet.addDistinct(m1);
                            newWorkingSet.addDistinct(m2);
                        }
                    } else {
                        if (priorities == null) {
                            // fallback impossible if priorities are missing
                            throw new IllegalStateException("Unable to introduce required " + (replacing ? "replacement" : "combination") + " rule (Missing dependencies for fallback).");
                        } else {
                            // priority-based fallback (higher priority wins by either getting promoted or replacing the other)
                            log.warn("Unable to introduce " + (replacing ? "replacement" : "combination") + " rule. Trying priority fallback.");
                            OpenFlowMessage promoted = null;
                            OpenFlowMessage other = null;
                            if (priorities.getPriority(m1.getHeader().getModuleId()) > priorities.getPriority(m1.getHeader().getModuleId())) {
                                if (!replacing)
                                    m1.setOfMessage(fM(m1).createBuilder().setPriority(Math.max(fM(m1).getPriority(), fM(m2).getPriority()) + 1).build());
                                promoted = m1;
                                other = m2;

                            } else if (priorities.getPriority(m1.getHeader().getModuleId()) < priorities.getPriority(m1.getHeader().getModuleId())) {
                                if (!replacing)
                                    m2.setOfMessage(fM(m2).createBuilder().setPriority(Math.max(fM(m1).getPriority(), fM(m2).getPriority()) + 1).build());
                                promoted = m2;
                                other = m1;
                            } else {
                                throw new IllegalStateException("Unable to introduce required " + (replacing ? "replacement" : "combination") + " rule (Insufficient dependencies for fallback).");
                            }
                            newWorkingSet.addDistinct(promoted);
                            if (!replacing)
                                newWorkingSet.addDistinct(other);
                            actions.put(promoted, replacing ? ResolutionAction.NONE : ResolutionAction.PROMOTED_AUTO);
                            actions.put(other, replacing ? ResolutionAction.REPLACED_AUTO : ResolutionAction.NONE);
                            log.debug("Successfully used priority fallback. One rule promoted.");
                        }
                    }
                }

            }
            workingSet = newWorkingSet;
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
     * Creates a new OpenFlowMessage from a given OFFlowMod.
     *
     * @param flowMod The OFFlowMod to encapsule.
     * @return An OpenFlowMessage wrapping the OFFlowMod.
     */
    private OpenFlowMessage messageFromFlowMod(OFFlowMod flowMod) {
        OpenFlowMessage newMessage = new OpenFlowMessage();
        newMessage.setOfMessage(flowMod);
        newMessage.setHeader(NetIPUtils.StubHeaderFromPayload(newMessage.getPayload()));
        newMessage.getHeader().setMessageType(MessageType.OPENFLOW);
        newMessage.getHeader().setModuleId(0); // TODO core indicator module id
        newMessage.getHeader().setDatapathId(0); // TODO calculate new value
        newMessage.getHeader().setTransactionId(0); // TODO calculate new value
        return newMessage;
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
        while (containsConflict(messages)) {
            for (int i = 0; i < messages.length; i++) {
                for (int j = i + 1; j < messages.length; j++) {
                    if (containsConflict(messages[i], messages[j])) {
                        int p1 = priorities.getPriority(messages[i].getHeader().getModuleId());
                        int p2 = priorities.getPriority(messages[j].getHeader().getModuleId());
                        if (p1 > p2) {
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
