package eu.netide.core.caos.composition;

import eu.netide.lib.netip.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class representing the result of an execution flow.
 *
 * Created by timvi on 07.08.2015.
 */
public class ExecutionResult {
    private List<Message> messagesToSend = new ArrayList<>();

    /**
     * Creates a new instance of the ExecutionResult class.
     */
    public ExecutionResult() {

    }

    /**
     * Adds a message to the list of messages to be sent to the shim.
     *
     * @param message The message to be added.
     */
    public void addMessageToSend(Message message) {
        messagesToSend.add(message);
    }

    /**
     * Gets a stream of messages to be sent to the shim.
     *
     * @return The stream of messages to be sent.
     */
    public Stream<Message> getMessagesToSend() {
        return messagesToSend.stream();
    }
}
