package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIPUtils;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.netide.core.caos.resolution.OFMatchConflict.Type.Same;
import static org.projectfloodlight.openflow.protocol.OFFlowModCommand.DELETE;
import static org.projectfloodlight.openflow.protocol.OFFlowModCommand.DELETE_STRICT;
import static org.projectfloodlight.openflow.protocol.match.MatchField.*;

/**
 * Class containing static utility methods for conflict resolution.
 * <p>
 * Created by timvi on 24.08.2015.
 */
public class ResolutionUtils {

    /**
     * Contains all match fields that are checked when checking for e.g. equivalence.
     */
    public static final MatchField[] MATCH_FIELDS_TO_CHECK;

    static {
        MATCH_FIELDS_TO_CHECK = new MatchField[]{
                IPV4_SRC,
                IPV4_DST,
                IPV6_SRC,
                IPV6_DST,
                ETH_SRC,
                ETH_DST,
                IN_PORT,
                IP_PROTO,
                ETH_TYPE,
                TCP_SRC,
                TCP_DST,
                UDP_SRC,
                UDP_DST
        };
    }

    /**
     * Gets a value indicating whether the given matches are equivalent. Matches are considered equivalent, if they expect the same values for each MatchField.
     *
     * @param match1 The first match.
     * @param match2 The second match.
     * @return True, if the matches are equivalent. False otherwise.
     */
    public static boolean areEquivalentMatches(Match match1, Match match2) {
        return ResolutionUtils.getMatchConflicts(null, null, match1, match2).allMatch(c -> c.getType() == Same);
    }

    /**
     * Gets a value indicating whether the given FlowMods are equivalent. FlowMods are considered equivalent if their matches are equivalent and their actions sets are equal.
     *
     * @param fm1 The first FlowMod.
     * @param fm2 The second FlowMod.
     * @return True, if the FlowMods are equivalent. False otherwise.
     * @see ResolutionUtils#areEquivalentMatches(Match, Match)
     */
    public static boolean areEquivalent(OFFlowMod fm1, OFFlowMod fm2) {
        return areEquivalentMatches(fm1.getMatch(), fm2.getMatch()) && fm1.getActions().stream().allMatch(a -> fm2.getActions().contains(a));
    }

    /**
     * Generates a candidate combined FlowMod out of the given two. The candidate represents the union of the matches and actions of the two given FlowMods.
     *
     * @param flowMod1 The first FlowMod.
     * @param flowMod2 The second FlowMod.
     * @return A candidate FlowMod representing the union of the given ones.
     */
    public static OFFlowMod generateCandidate(OFFlowMod flowMod1, OFFlowMod flowMod2) {
        Match.Builder candidateMatchBuilder = flowMod1.getMatch().createBuilder();
        getMatchConflicts(messageFromFlowMod(flowMod1), messageFromFlowMod(flowMod2), flowMod1.getMatch(), flowMod2.getMatch()).forEach(c -> {
            MatchField matchField = c.getMatchField();
            Match m = ((OFFlowMod) ((OpenFlowMessage) c.getMessage2()).getOfMessage()).getMatch();
            if (m.isExact(matchField)) {
                candidateMatchBuilder.setExact(matchField, m.get(matchField));
            } else {
                candidateMatchBuilder.setMasked(matchField, m.getMasked(matchField));
            }
        });
        OFFlowMod candidate = OFFactories.getFactory(OFVersion.OF_10).buildFlowModify()
                .setActions(Stream.concat(flowMod1.getActions().stream(), flowMod2.getActions().stream()).collect(Collectors.toSet()).stream().collect(Collectors.toList()))
                .setMatch(candidateMatchBuilder.build())
                .setPriority(Math.max(flowMod1.getPriority(), flowMod2.getPriority()) + 1).build();
        // TODO calculate priority, set remaining important fields (?), check OF version
        return candidate;
    }

    /**
     * Creates a new OpenFlowMessage from a given OFFlowMod.
     *
     * @param flowMod The OFFlowMod to encapsule.
     * @return An OpenFlowMessage wrapping the OFFlowMod.
     */
    public static OpenFlowMessage messageFromFlowMod(OFFlowMod flowMod) {
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
     * Gets a stream of all conflicts between the given messages.
     *
     * @param message1 The first message.
     * @param message2 The second message.
     * @param match1   The match of the first message.
     * @param match2   The match of the second message.
     * @return A stream of detected conflicts.
     */
    public static Stream<OFMatchConflict> getMatchConflicts(OpenFlowMessage message1, OpenFlowMessage message2, Match match1, Match match2) {

        List<OFMatchConflict> foundConflicts = new ArrayList<>();

        for (MatchField mf : MATCH_FIELDS_TO_CHECK) {
            if (!match1.isFullyWildcarded(mf) && !match2.isFullyWildcarded(mf)) {
                if (match1.isExact(mf) && match2.isExact(mf)) {
                    if (match1.get(mf).compareTo(match2.get(mf)) == 0) {
                        foundConflicts.add(new OFMatchConflict(message1, message2, mf, OFMatchConflict.Type.Same));
                    }
                } else if (match1.isExact(mf) && match2.isPartiallyMasked(mf)) {
                    if ((match2.getMasked(mf)).matches(match1.get(mf))) {
                        foundConflicts.add(new OFMatchConflict(message2, message1, mf, OFMatchConflict.Type.Superset));
                    }
                } else if (match1.isPartiallyMasked(mf) && match2.isExact(mf)) {
                    if ((match1.getMasked(mf)).matches(match2.get(mf))) {
                        foundConflicts.add(new OFMatchConflict(message1, message2, mf, OFMatchConflict.Type.Superset));
                    }
                }
            } else if (match1.isFullyWildcarded(mf) && !match2.isFullyWildcarded(mf)) {
                foundConflicts.add(new OFMatchConflict(message1, message2, mf, OFMatchConflict.Type.Superset));
            } else if (!match1.isFullyWildcarded(mf) && match2.isFullyWildcarded(mf)) {
                foundConflicts.add(new OFMatchConflict(message2, message1, mf, OFMatchConflict.Type.Superset));
            }
        }

        return foundConflicts.stream();
    }

    /**
     * Gets a value indicating whether the given FlowMod messages create a conflict in their action sets.
     *
     * @param offm1 The first OFFlowMod.
     * @param offm2 The second OFFlowMod.
     * @return True, if a conflict was found. False otherwise.
     */
    public static boolean getActionConflicts(OFFlowMod offm1, OFFlowMod offm2) {
        // only possible on following command constellations: ADD-ADD, ADD-MODIFY, MODIFY-MODIFY, MODIFY-DELETE (or NOT DELETE-DELETE)
        OFFlowModCommand c1 = offm1.getCommand();
        OFFlowModCommand c2 = offm2.getCommand();
        if (c1 != DELETE && c1 != DELETE_STRICT && c2 != DELETE && c2 != DELETE_STRICT) {
            return offm1.getActions().stream().anyMatch(a -> offm2.getActions().stream().anyMatch(a::equals));
        }
        return false;
    }
}
