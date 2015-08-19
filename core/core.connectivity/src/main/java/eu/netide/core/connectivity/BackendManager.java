package eu.netide.core.connectivity;

import eu.netide.core.api.*;
import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.ManagementMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * BackendManager implementation for the core.
 *
 * Created by timvi on 11.08.2015.
 */
public class BackendManager implements IBackendManager, IConnectorListener {

    private static final Logger logger = LoggerFactory.getLogger(BackendManager.class);

    private IBackendConnector connector;
    private List<IBackendMessageListener> backendMessageListeners;
    private Semaphore listenerLock = new Semaphore(1);
    private List<String> backendIds = new ArrayList<>();
    private Dictionary<Integer, String> moduleMappings = new Hashtable<>();

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Dictionary<Integer, RequestResult> locks = new Hashtable<>();

    /**
     * Called by Apache Aries on startup (configured in blueprint.xml)
     */
    public void Start() {
        logger.info("BackendManager started.");
    }

    /**
     * Called by Apache Aries on shutdown (configured in blueprint.xml)
     */
    public void Stop() {
        logger.info("BackendManager stopped.");
    }

    @Override
    public boolean sendMessage(Message message) {
        return connector.SendData(message.toByteRepresentation(), getBackend(message.getHeader().getModuleId()));
    }

    @Override
    public RequestResult sendRequest(Message message) {
        Integer id = message.getHeader().getModuleId();
        if (locks.get(id) != null) {
            throw new IllegalStateException("Still waiting for former request to finish.");
        }
        RequestResult result = new RequestResult(message);
        locks.put(id, result);
        sendMessage(message);

        while (!result.isDone()) {
            try {
                result.wait();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
        locks.remove(id);
        return result;
    }

    @Override
    public Future<RequestResult> sendRequestAsync(Message message) {
        return pool.submit(() -> sendRequest(message));
    }

    @Override
    public Iterable<String> getBackendIds() {
        return backendIds;
    }

    @Override
    public Iterable<Integer> getModules() {
        return Collections.list(moduleMappings.keys());
    }

    @Override
    public String getBackend(Integer moduleId) {
        return moduleMappings.get(moduleId);
    }

    @Override
    public void OnDataReceived(byte[] data, String backendId) {
        Message message = NetIPConverter.parseConcreteMessage(data);

        RequestResult r = locks.get(message.getHeader().getModuleId());
        if (r != null && r.getRequestMessage().getHeader().getTransactionId() == message.getHeader().getTransactionId()) {
            // Message belongs to former request
            if (message instanceof ManagementMessage) {
                // check for finished execution
                // TODO real check
                r.signalIsDone((ManagementMessage) message);
                r.notify();
            } else {
                // add to result
                r.addResultMessage(message);
            }
        } else {
            // Random message from backend
            if (message instanceof HelloMessage) {
                // remember backend and mapping
                if (!backendIds.stream().anyMatch(a -> a.equals(backendId))) {
                    backendIds.add(backendId);
                }
                moduleMappings.put(message.getHeader().getModuleId(), backendId);
                // TODO handle message appropriately
            } else if (message instanceof ManagementMessage) {
                // TODO handle appropriately
            } else {
                try {
                    listenerLock.acquire();
                    for (IBackendMessageListener listener : backendMessageListeners) {
                        listener.OnBackendMessage(message, backendId);
                    }
                } catch (InterruptedException e) {
                    logger.error("", e);
                } finally {
                    listenerLock.release();
                }
            }
        }
    }

    /**
     * Setter for injecting the connector using Aries.
     *
     * @param connector The connector.
     */
    public void setConnector(IBackendConnector connector) {
        this.connector = connector;
        connector.RegisterBackendListener(this);
    }

    /**
     * Setter for injecting the list of message listeners using Aries.
     *
     * @param backendMessageListeners The list of backend message listeners.
     * @throws InterruptedException
     */
    public void setBackendMessageListeners(List<IBackendMessageListener> backendMessageListeners) throws InterruptedException {
        listenerLock.acquire();
        this.backendMessageListeners = backendMessageListeners == null ? new ArrayList<>() : backendMessageListeners;
        listenerLock.release();
    }
}
