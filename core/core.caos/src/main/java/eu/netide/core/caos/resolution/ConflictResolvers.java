package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by timvi on 24.08.2015.
 */
public class ConflictResolvers {
    public static final IConflictResolver DEFAULT_OF_CONFLICTRESOLVER = new DefaultOFConflictResolver();

    private static final IConflictResolver[] allResolvers = new IConflictResolver[]{DEFAULT_OF_CONFLICTRESOLVER};

    public static IConflictResolver getDefaultConflictResolver(MessageType messageType) {
        switch (messageType) {
            case OPENFLOW:
                return DEFAULT_OF_CONFLICTRESOLVER;
            default:
                throw new UnsupportedOperationException("No default conflict resolver known for MessageType '" + messageType.name() + "'");
        }
    }

    public static IConflictResolver getMatchingResolver(Message[] messages) {
        Optional<IConflictResolver> resolver = Arrays.stream(allResolvers)
                .filter(r ->
                        Arrays.stream(messages)
                                .allMatch(m -> Arrays.stream(r.getSupportedMessageTypes())
                                        .anyMatch(mt -> mt == m.getHeader().getMessageType())))
                .findFirst();
        if (!resolver.isPresent()) {
            throw new UnsupportedOperationException("No matching resolver found.");
        }
        return resolver.get();
    }
}
