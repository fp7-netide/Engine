package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.Message;

import java.util.Dictionary;

/**
 * Created by timvi on 24.08.2015.
 */
public class ResolutionResult {
    private Message[] resultingMessagesToSend;
    private Dictionary<Message, ResolutionAction> takenActions;

    public ResolutionResult(Message[] resultingMessagesToSend, Dictionary<Message, ResolutionAction> takenActions) {
        this.resultingMessagesToSend = resultingMessagesToSend;
        this.takenActions = takenActions;
    }

    public Message[] getResultingMessagesToSend() {
        return resultingMessagesToSend;
    }

    public Dictionary<Message, ResolutionAction> getTakenActions() {
        return takenActions;
    }
}
