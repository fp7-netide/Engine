package eu.netide.core.connectivity;

import eu.netide.core.api.*;
import eu.netide.lib.netip.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

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
    private Map<Integer, String> moduleToBackendMappings = new HashMap<>();
    private Map<Integer, String> moduleToNameMappings = new HashMap<>();

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Dictionary<Integer, RequestResult> results = new Hashtable<>();
    private final Dictionary<Integer, Semaphore> locks = new Hashtable<>();

    private Random random = new Random();

    /**
     * Called by Apache Aries on startup (configured in blueprint.xml)
     */
    public void Start() {
        logger.debug("BackendManager started.");
    }

    /**
     * Called by Apache Aries on shutdown (configured in blueprint.xml)
     */
    public void Stop() {
        logger.debug("BackendManager stopped.");
    }

    @Override
    public boolean sendMessage(Message message) {
        logger.info("Sending message '" + message.toString() + "' to backend '" + getBackend(message.getHeader().getModuleId()) + "'.");
        return connector.SendData(message.toByteRepresentation(), getBackend(message.getHeader().getModuleId()));
    }

    @Override
    public RequestResult sendRequest(Message message) {
        Integer id = message.getHeader().getModuleId();
        if (results.get(id) != null) {
            throw new IllegalStateException("Still waiting for former request to finish.");
        }
        RequestResult result = new RequestResult(message);
        Semaphore lock = new Semaphore(0);
        results.put(id, result);
        locks.put(id, lock);
        sendMessage(message);

        while (!result.isDone()) {
            logger.info("Waiting for request with id '" + id + "' to complete...");
            try {
                lock.acquire();
            } catch (InterruptedException e) {
                logger.error("InterruptedException occurred while waiting for results", e);
            }
        }
        results.remove(id);
        locks.remove(id);
        return result;
    }

    @Override
    public Future<RequestResult> sendRequestAsync(Message message) {
        return pool.submit(() -> sendRequest(message));
    }

    @Override
    public Stream<String> getBackendIds() {
        return backendIds.stream();
    }

    @Override
    public Stream<Integer> getModuleIds() {
        return moduleToBackendMappings.keySet().stream();
    }

    @Override
    public Stream<String> getModules() {
        return moduleToNameMappings.values().stream();
    }

    @Override
    public String getBackend(Integer moduleId) {
        if (moduleToBackendMappings.containsKey(moduleId))
            return moduleToBackendMappings.get(moduleId);
        else
            throw new UnsupportedOperationException("No backend mapping known for moduleId '" + moduleId + "'.");
    }

    public int getModuleId(String moduleName) {
        return moduleToNameMappings.keySet().stream().filter(key -> Objects.equals(moduleToNameMappings.get(key), moduleName)).findFirst().get();
    }

    @Override
    public void OnDataReceived(byte[] data, String backendId) {
        Message message;
        try {
            message = NetIPConverter.parseConcreteMessage(data);
        } catch (Exception ex) {
            logger.error("Unable to parse received data from '" + backendId + "' (" + Arrays.toString(data) + ").", ex);
            return;
        }
        int id = message.getHeader().getModuleId();

        logger.info("Data received from backend '" + backendId + "' with moduleId '" + message.getHeader().getModuleId() + "'.");

        RequestResult r = results.get(id);
        Semaphore lock = locks.get(id);
        if (r != null && lock != null && r.getRequestMessage().getHeader().getTransactionId() == message.getHeader().getTransactionId()) {
            // Message belongs to former request
            if (message instanceof ManagementMessage) {
                // check for finished execution
                // TODO real check
                r.signalIsDone((ManagementMessage) message);
                lock.release();
                logger.info("Data completes request (" + message.toString() + ").");
            } else {
                // add to result
                r.addResultMessage(message);
                logger.info("Message adds to running request (" + message.toString() + ").");
            }
        } else {
            // Random message from backend
            if (message instanceof HelloMessage) {
                // just relay it to the shim
                logger.info("Received HelloMessage from backend, relaying to shim...");
                connector.SendData(message.toByteRepresentation(), "shim"); // TODO add shim constant
                return;
            } else if (message instanceof ModuleAnnouncementMessage) {
                // remember backend
                if (!backendIds.stream().anyMatch(a -> a.equals(backendId))) {
                    backendIds.add(backendId);
                }
                // get new module ID
                ModuleAnnouncementMessage mam = (ModuleAnnouncementMessage) message;
                logger.info("Received ModuleAnnouncement for module '" + mam.getModuleName() + "' from backend '" + backendId + "'. Calculating ID...");
                int moduleId = random.nextInt(1000); // for easier typing in the emulator, restrict to smaller numbers
                while (moduleToNameMappings.keySet().contains(moduleId) || moduleId < 1) {
                    moduleId = random.nextInt(1000);
                }
                moduleToNameMappings.put(moduleId, mam.getModuleName());
                moduleToBackendMappings.put(moduleId, backendId);
                // send acknowledge back
                ModuleAcknowledgeMessage ack = new ModuleAcknowledgeMessage();
                ack.setModuleName(mam.getModuleName());
                ack.setHeader(NetIPUtils.StubHeaderFromPayload(ack.getPayload()));
                ack.getHeader().setMessageType(MessageType.MODULE_ACKNOWLEDGE);
                ack.getHeader().setModuleId(moduleId);
                connector.SendData(ack.toByteRepresentation(), backendId);
                logger.info("Mapped module '" + mam.getModuleName() + "' to id '" + moduleId + "' and sent ModuleAcknowledgeMessage to backend '" + backendId + "'.");
            } else if (message instanceof ManagementMessage) {
                logger.info("Received unrequested ManagementMessage: '" + message.toString() + "'. Relaying to shim.");
                connector.SendData(message.toByteRepresentation(), "shim"); // TODO make shim name a constant
            } else {
                logger.info("Received unrequested Message: '" + message.toString() + "'. Relaying to shim.");
                try {
                    listenerLock.acquire();
                    // Notify listeners and send to shim
                    for (IBackendMessageListener listener : backendMessageListeners) {
                        pool.submit(() -> listener.OnBackendMessage(message, backendId));
                    }
                    connector.SendData(message.toByteRepresentation(), "shim"); // TODO make shim name a constant
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
