package eu.netide.core.caos.composition;

import eu.netide.lib.netip.Message;

/**
 * Class representing the current status of an execution flow.
 *
 * Created by timvi on 07.08.2015.
 */
public class ExecutionFlowStatus {
    private Message originalMessage;
    private Message currentMessage;

    /**
     * Creates a new instance of the ExecutionFlowStatus class.
     *
     * @param originalMessage The message that triggered the start of the execution flow.
     */
    public ExecutionFlowStatus(Message originalMessage) {
        this.originalMessage = originalMessage;
    }

    /**
     * Gets the original message.
     *
     * @return The original message.
     */
    public Message getOriginalMessage() {
        return originalMessage;
    }

    /**
     * Gets the current message.
     *
     * @return The current message.
     */
    public Message getCurrentMessage() {
        return currentMessage;
    }

    /**
     * Sets the current message.
     *
     * @param currentMessage The new current message, reflecting the state of execution up to this point.
     */
    public void setCurrentMessage(Message currentMessage) {
        this.currentMessage = currentMessage;
    }
}
