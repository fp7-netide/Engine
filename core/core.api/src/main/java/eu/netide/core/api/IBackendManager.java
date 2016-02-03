package eu.netide.core.api;

import eu.netide.lib.netip.Message;

import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Interface for BackendManagers.
 *
 * Created by timvi on 07.08.2015.
 */
public interface IBackendManager {

    /**
     * Directly sends the given message.
     *
     * @param message The message to send.
     * @return True, if sending was successful. False otherwise.
     */
    boolean sendMessage(Message message);

    /**
     * Directly sends the given message to all backend.
     *
     * @param message The message to send.
     * @return True, if sending was successful. False otherwise.
     */
    boolean sendMessageAllBackends(Message message);




    /**
     * Sends a request as specified in the given message and waits for the execution results.
     *
     * @param message The request message.
     * @return The result of the request.
     */
    RequestResult sendRequest(Message message);

    /**
     * Sends a request as specified in the given message and asynchronously waits for the results.
     *
     * @param message The request message.
     * @return The result of the request.
     */
    Future<RequestResult> sendRequestAsync(Message message);

    /**
     * Gets a list of all known backend ids.
     *
     * @return The list of backend ids.
     */
    Stream<String> getBackendIds();

    /**
     * Returns the list of all known module ids.
     *
     * @return The list of module ids.
     */
    Stream<Integer> getModuleIds();

    /**
     * Returns the list of all known module names.
     *
     * @return The list of module names.
     */
    Stream<String> getModules();

    /**
     * Gets the backend id for a given module id.
     *
     * @param moduleId the module id.
     * @return The corresponding backend id.
     */
    String getBackend(Integer moduleId) throws NoSuchElementException;

    /**
     * Gets the moduleId from a given module name.
     *
     * @param moduleName The module name.
     * @return The module id.
     */
    int getModuleId(String moduleName) throws NoSuchElementException;

    //! This methods marks a module as finished even though no fence message has been received
    void markModuleAllOutstandingRequestsAsFinished(int moduleId);
}
