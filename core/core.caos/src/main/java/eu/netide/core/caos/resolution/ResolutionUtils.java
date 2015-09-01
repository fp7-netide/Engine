package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.projectfloodlight.openflow.protocol.OFFlowModCommand.DELETE;
import static org.projectfloodlight.openflow.protocol.OFFlowModCommand.DELETE_STRICT;

/**
 * Class containing static utility methods for conflict resolution.
 * <p>
 * Created by timvi on 24.08.2015.
 */
public class ResolutionUtils {

    private static final MatchField[] MATCH_FIELDS_TO_CHECK = new MatchField[]{
            MatchField.IPV4_SRC,
            MatchField.IPV4_DST,
            MatchField.IPV6_SRC,
            MatchField.IPV6_DST,
            MatchField.ETH_SRC,
            MatchField.ETH_DST,
            MatchField.IN_PORT,
            MatchField.IP_PROTO,
            MatchField.ETH_TYPE,
            MatchField.TCP_SRC,
            MatchField.TCP_DST,
            MatchField.UDP_SRC,
            MatchField.UDP_DST
    };

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
                        foundConflicts.add(new OFMatchConflict(message1, message2, mf, OFMatchConflict.Type.Incompatible));
                    }
                } else if (match1.isExact(mf) && match2.isPartiallyMasked(mf)) {
                    if ((match2.getMasked(mf)).matches(match1.get(mf))) {
                        foundConflicts.add(new OFMatchConflict(message1, message2, mf, OFMatchConflict.Type.Compatible));
                    }
                } else if (match1.isPartiallyMasked(mf) && match2.isExact(mf)) {
                    if ((match1.getMasked(mf)).matches(match2.get(mf))) {
                        foundConflicts.add(new OFMatchConflict(message1, message2, mf, OFMatchConflict.Type.Compatible));
                    }
                }
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
            return offm1.getActions().stream().anyMatch(a -> offm2.getActions().stream().anyMatch(b -> a == b));
        }
        return false;
    }
}
