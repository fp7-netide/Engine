package eu.netide.core.caos.resolution;

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
     * @param message1 The first message of the conflict.
     * @param message2 The second message of the conflict.
     * @param field    The Matchfield that creates the conflict.
     * @param type     The type of the conflict.
     */
    public OFMatchConflict(OpenFlowMessage message1, OpenFlowMessage message2, MatchField field, OFMatchConflict.Type type) {
        this.message1 = message1;
        this.message2 = message2;
        this.matchField = field;
        this.type = type;
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
        Compatible,
        /**
         * Indicates that the conflict is incompatible, i.e. that the matches directly contradict each other.
         */
        Incompatible
    }
}
