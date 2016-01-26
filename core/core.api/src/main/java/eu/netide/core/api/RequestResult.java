package eu.netide.core.api;

import eu.netide.lib.netip.FenceMessage;
import eu.netide.lib.netip.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class representing the results of a backend request.
 * <p>
 * Created by timvi on 17.08.2015.
 */
public class RequestResult {

    private boolean isDone = false;
    private Message requestMessage;
    private List<Message> resultMessages = new ArrayList<>();
    private FenceMessage finishMessage;

    /**
     * Creates a new instance of the RequestResult class.
     *
     * @param requestMessage The message containing the request.
     */
    public RequestResult(Message requestMessage) {
        this.requestMessage = requestMessage;
    }

    /**
     * Adds the given message to the set of result messages, If the result has already been marked as finished, this throws an exception.
     *
     * @param message The message.
     */
    public void addResultMessage(Message message) {
        if (isDone) {
            throw new IllegalStateException("Cannot add result messages after setting state to done.");
        }
        this.resultMessages.add(message);
    }

    /**
     * Gets all messages that were identified as results.
     *
     * @return An enumeration of result messages.
     */
    public Stream<Message> getResultMessages() {
        return this.resultMessages.stream();
    }

    /**
     * Gets the original request message.
     *
     * @return The original request message.
     */
    public Message getRequestMessage() {
        return this.requestMessage;
    }

    /**
     * Gets the ManagementMessage that caused this request to be marked as finished.
     *
     * @return The finishing ManagementMessage.
     */
    public FenceMessage getFinishMessage() {
        return this.finishMessage;
    }

    /**
     * Gets a value indicating whether all results for this request have been collected.
     *
     * @return True, if all results have been collected. False otherwise.
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Marks this RequestResult as completed and sets the ManagementMessage that caused this. If the result is already marked as finished, this throws an exception.
     *
     * @param finishMessage The ManagementMessage that caused this result to be marked as finished.
     */
    public void signalIsDone(FenceMessage finishMessage) {
        if (isDone) {
            throw new IllegalStateException("Cannot set finished state more than once.");
        }
        this.finishMessage = finishMessage;
        isDone = true;
    }
}
