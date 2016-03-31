package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.match.MatchField;

/**
 * Class representing an OpenFlow Match conflict.
 * <p>
 * Created by timvi on 31.08.2015.
 */
public class OFMatchConflict extends Conflict {
    private MatchField matchField;
    private OFMatchConflict.Type type;

    /**
     * Creates a new instance of the OFMatchConflict class.
     *
     * @param maskedOrExactMessage The first message of the conflict.
     * @param exactMessage The second message of the conflict.
     * @param field    The Matchfield that creates the conflict.
     * @param type     The type of the conflict.
     */
    public OFMatchConflict(OpenFlowMessage maskedOrExactMessage, OpenFlowMessage exactMessage, MatchField field, OFMatchConflict.Type type) {
        this.message1 = maskedOrExactMessage;
        this.message2 = exactMessage;
        this.matchField = field;
        this.type = type;
    }

    /**
     * Returns the first message which contains a masked match creating the conflict. If neither of both messages contains the masked field, this returns the first message.
     *
     * @return The message containing the matchfield masked or exact.
     */
    public Message getMaskedOrExactMessage() {
        return this.message1;
    }

    /**
     * Gets the conflicting Matchfield.
     *
     * @return The Matchfield.
     */
    public MatchField getMatchField() {
        return this.matchField;
    }

    /**
     * Gets the conflict type.
     *
     * @return The conflict type.
     */
    public OFMatchConflict.Type getType() {
        return this.type;
    }

    public enum Type {
        /**
         * Indicates that the conflict is compatible, i.e. that one match is a superset of the other.
         */
        Superset,
        /**
         * Indicates that the conflict is incompatible, i.e. that the matches directly contradict each other.
         */
        Same
    }
}
