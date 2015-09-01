package eu.netide.core.caos.resolution;

import eu.netide.core.caos.composition.ResolutionPolicy;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;

/**
 * Interface for conflict resolvers.
 * <p>
 * Created by timvi on 24.08.2015.
 */
public interface IConflictResolver {
    /**
     * Gets an array of all message types supported for conflict resolution. Note that this does not imply that cross-type conflicts can be resolved!
     *
     * @return The array of supported message types.
     */
    MessageType[] getSupportedMessageTypes();

    /**
     * Gets an array of supported resolution policies.
     *
     * @return The array of supported resolution policies.
     */
    ResolutionPolicy[] getSupportedResolutionPolicies();

    /**
     * Resolves eventual conflicts between the existingMessages and the newMessages and merges them to one set,
     * assuming that existing messages have a higher priority.
     *
     * @param existingMessages The existing messages.
     * @param newMessages      The new messages that should be merged with the existing.
     * @return The result of the resolution.
     */
    ResolutionResult resolve(Message[] existingMessages, Message[] newMessages);

    /**
     * Resolves eventual conflicts between the existingMessages and the newMessages and merges them to one set,
     * preferring existing or new messages based on the switch.
     *
     * @param existingMessages The existing messages.
     * @param newMessages      The new messages that should be merged with the existing.
     * @param preferExisting   If true, existing messages will take priority over new messages.
     * @return The result of the resolution.
     */
    ResolutionResult resolve(Message[] existingMessages, Message[] newMessages, boolean preferExisting);

    /**
     * Resolves any conflicts in the given set of messages using the specified policy and priority information.
     *
     * @param messages   The set of messages to resolve.
     * @param policy     The ResolutionPolicy to use.
     * @param priorities The PriorityInfo to use. Used only for PRIORITY and (optionally) AUTO policies. Can be null for other policies.
     * @return The result of the resolution.
     */
    ResolutionResult resolve(Message[] messages, ResolutionPolicy policy, PriorityInfo priorities);

    /**
     * Gets a value indicating whether conflicts exist in the given set of messages.
     *
     * @param messages The set of messages to check.
     * @return True, if conflicts where found. False otherwise.
     */
    boolean containsConflict(Message[] messages);

    /**
     * Gets a value indicating whether a conflict exists between the two given messages.
     *
     * @param message1 The first message.
     * @param message2 The second message.
     * @return True, if a conflict was found. False otherwise.
     */
    boolean containsConflict(Message message1, Message message2);
}
