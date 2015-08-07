package eu.netide.core.caos.composition;

import eu.netide.core.api.netip.Message;

/**
 * Created by timvi on 07.08.2015.
 */
public class ExecutionFlowStatus {
    private Message originalMessage;
    private Message currentMessage;

    public ExecutionFlowStatus(Message originalMessage) {
        this.originalMessage = originalMessage;
    }

    public Message getOriginalMessage() {
        return originalMessage;
    }

    public Message getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(Message currentMessage) {
        this.currentMessage = currentMessage;
    }
}
