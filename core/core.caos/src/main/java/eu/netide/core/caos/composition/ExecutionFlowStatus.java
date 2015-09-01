package eu.netide.core.caos.composition;

import eu.netide.lib.netip.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing the current status of an execution flow.
 *
 * Created by timvi on 07.08.2015.
 */
public class ExecutionFlowStatus {
    private Message originalMessage;
    private Message currentMessage;
    private List<Message> resultMessages;

    /**
     * Creates a new instance of the ExecutionFlowStatus class.
     *
     * @param originalMessage The message that triggered the start of the execution flow.
     */
    public ExecutionFlowStatus(Message originalMessage) {
        this.originalMessage = originalMessage;
        this.resultMessages = new ArrayList<>();
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

    /**
     * Returns the list of result messages that have been returned so far.
     *
     * @return The list of result messages.
     */
    public List<Message> getResultMessages() {
        return resultMessages;
    }

    /**
     * Sets the list of result messges.
     *
     * @param resultMessages The new list of result messages.
     */
    public void setResultMessages(List<Message> resultMessages) {
        this.resultMessages = resultMessages;
    }
}
