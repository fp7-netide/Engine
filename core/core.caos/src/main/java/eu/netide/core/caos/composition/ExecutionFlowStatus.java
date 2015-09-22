package eu.netide.core.caos.composition;

import eu.netide.lib.netip.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing the current status of an execution flow.
 *
 * Created by timvi on 07.08.2015.
 */
public class ExecutionFlowStatus {
    private Message originalMessage;
    private Message currentMessage;
    private Map<Long, List<Message>> resultMessages;

    /**
     * Creates a new instance of the ExecutionFlowStatus class.
     *
     * @param originalMessage The message that triggered the start of the execution flow.
     */
    public ExecutionFlowStatus(Message originalMessage) {
        this.originalMessage = originalMessage;
        this.currentMessage = originalMessage;
        this.resultMessages = new HashMap<>();
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
     * @param currentMessage The new set of current messages, reflecting the state of execution up to this point.
     */
    public void setCurrentMessage(Message currentMessage) {
        this.currentMessage = currentMessage;
    }

    /**
     * Returns the list of result messages that have been returned so far.
     *
     * @return The list of result messages.
     */
    public Map<Long, List<Message>> getResultMessages() {
        return resultMessages;
    }

    /**
     * Sets the list of result messges.
     *
     * @param resultMessages The new list of result messages.
     */
    public void setResultMessages(Map<Long, List<Message>> resultMessages) {
        this.resultMessages = resultMessages;
    }

    /**
     * Adds a new result message for the given datapath.
     *
     * @param datapathId The datapath ID.
     * @param message    The message.
     */
    public void addResultMessage(long datapathId, Message message) {
        if (resultMessages.containsKey(datapathId)) {
            resultMessages.get(datapathId).add(message);
        } else {
            List<Message> lst = new ArrayList<>();
            lst.add(message);
            resultMessages.put(datapathId, lst);
        }
    }

    public ExecutionFlowStatus clone(Message originalMessage) {
        ExecutionFlowStatus clonedStatus = new ExecutionFlowStatus(originalMessage);
        Map<Long, List<Message>> clonedMap = new HashMap<>();
        for (Long dpid : this.getResultMessages().keySet()) {
            clonedMap.put(dpid, (List<Message>) ((ArrayList<Message>) this.getResultMessages().get(dpid)).clone());
        }
        clonedStatus.setResultMessages(clonedMap);
        return clonedStatus;
    }
}
