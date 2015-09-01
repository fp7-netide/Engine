package eu.netide.core.caos.resolution;

/**
 * Created by timvi on 24.08.2015.
 */
public enum ResolutionAction {
    IGNORED("ignored"),
    CREATED_AUTO("created by automerge"),
    PROMOTED_AUTO("promoted by automerge"),
    NONE("none");


    private final String description;

    ResolutionAction(String v) {
        description = v;
    }

    public String getDescription() {
        return description;
    }

    public static ResolutionAction fromDescription(String v) {
        for (ResolutionAction c : ResolutionAction.values()) {
            if (c.description.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
