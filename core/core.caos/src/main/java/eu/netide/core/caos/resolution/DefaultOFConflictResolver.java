package eu.netide.core.caos.resolution;

import eu.netide.core.caos.composition.ResolutionPolicy;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFType;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.stream.Stream;

/**
 * Default implementation of IConflictResolver for OpenFlow messages.
 * <p>
 * Created by timvi on 24.08.2015.
 */
public class DefaultOFConflictResolver implements IConflictResolver {

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
        return null;
    }

    @Override
    public ResolutionResult resolve(Message[] existingMessages, Message[] newMessages, boolean preferExisting) {
        return null;
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

        if (m1.getOfMessage().getType() == OFType.FLOW_MOD && m2.getOfMessage().getType() == OFType.FLOW_MOD) {
            OFFlowMod fm1 = (OFFlowMod) m1.getOfMessage();
            OFFlowMod fm2 = (OFFlowMod) m2.getOfMessage();

            Stream<OFMatchConflict> result = ResolutionUtils.getMatchConflicts(m1, m2, fm1.getMatch(), fm2.getMatch());
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
        return null; // TODO resolveAuto
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
