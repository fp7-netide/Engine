package eu.netide.core.connectivity;

import eu.netide.core.api.*;
import eu.netide.lib.netip.*;
import org.javatuples.Pair;
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
 * <p>
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
    private Map<Integer, Long> moduleLastMessage = new HashMap<>();

    private final ExecutorService pool = Executors.newCachedThreadPool();


    static class PendingRequest {
        private final Semaphore lock;
        private final RequestResult result;

        PendingRequest(RequestResult requestResult, Semaphore sem) {
            result = requestResult;
            lock = sem;
        }
    }

    private final Map<Pair<Integer, Integer>, PendingRequest> requests = new Hashtable<>();

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
    public boolean sendMessageAllBackends(Message message) {
        boolean success = true;
        logger.info("Sending message '" + message.toString() + "' to  all backends ");
        for (String backendId : backendIds)
            success = connector.SendData(message.toByteRepresentation(), backendId) && success;

        return success;
    }


    @Override
    public RequestResult sendRequest(Message message) {
        RequestResult result;

        int moduleId;
        Semaphore requestLock;
        Pair<Integer, Integer> requestKey;
        synchronized (requests) {
            moduleId = message.getHeader().getModuleId();
            int xid = message.getHeader().getTransactionId();
            requestKey = Pair.with(moduleId, xid);
            if (requests.get(requestKey) != null) {
                throw new IllegalStateException("Still waiting for former request to finish.");
            }
            result = new RequestResult(message);
            requestLock = new Semaphore(0);
            requests.put(requestKey, new PendingRequest(result, requestLock));
        }
        sendMessage(message);

        while (!result.isDone()) {
            logger.info("Waiting for request with id '" + moduleId + "' to complete...");
            try {
                requestLock.acquire();
            } catch (InterruptedException e) {
                logger.error("InterruptedException occurred while waiting for results", e);
            }
        }
        synchronized (requests) {
            requests.remove(requestKey);
        }
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
    public String getModuleName(Integer moduleId) throws NoSuchElementException {
        return moduleToNameMappings.get(moduleId);
    }

    @Override
    public String getBackend(Integer moduleId) {
        if (moduleToBackendMappings.containsKey(moduleId)) {
            return moduleToBackendMappings.get(moduleId);
        } else {
            throw new UnsupportedOperationException("Backend mapping unknown for moduleId '" + moduleId + "'.");
        }
    }

    @Override
    public Long getLastMessageTime(Integer moduleId) {
        return moduleLastMessage.get(moduleId);
    }

    public int getModuleId(String moduleName) {
        return moduleToNameMappings.keySet().stream().filter(key -> Objects.equals(moduleToNameMappings.get(key), moduleName)).findFirst().get();
    }

    //! This methods marks a module as finished even though no fence message has been received
    @Override
    public void markModuleAllOutstandingRequestsAsFinished(int moduleId) {
        synchronized (requests) {
            for (Map.Entry<Pair<Integer, Integer>, PendingRequest> item : requests.entrySet()) {
                if (item.getKey().getValue0() == moduleId && !item.getValue().result.isDone()) {
                    FenceMessage fakeFenceMessage = new FenceMessage();
                    fakeFenceMessage.setPayload("NO FENCE SUPPORT FAKE MSG".getBytes());
                    item.getValue().result.signalIsDone(fakeFenceMessage);
                    item.getValue().lock.release();
                }
            }
        }
    }

    /* TODO: Probably more places to remove old backend */
    @Override
    public void removeBackend(int id)
    {
        String backEndName = getBackend(id);
        logger.info("Removing backend %s", backEndName);

        LinkedList<Integer> removedModules= new LinkedList<>();
        moduleToBackendMappings.entrySet().forEach(
                (modID) -> {
                    if (modID.getValue().equals(backEndName)) {
                        moduleToNameMappings.remove(modID.getKey());
                        moduleLastMessage.remove(modID.getKey());
                        removedModules.add(modID.getKey());
                    }
                });

        removedModules.forEach((key) -> {
            moduleToBackendMappings.remove(key);
            backendIds.remove(backEndName);
        });

        try {
            listenerLock.acquire();
            // Notify listeners and send to shim
            for (IBackendMessageListener listener : backendMessageListeners) {
                pool.submit(() -> listener.OnBackendRemoved(backEndName, removedModules));
            }
        } catch (InterruptedException e) {
            logger.error("", e);
        } finally {
            listenerLock.release();
        }
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

        moduleLastMessage.put(id, System.currentTimeMillis());

        logger.info("Data received from backend '" + backendId + "' with moduleId '" + message.getHeader().getModuleId() + "'.");

        synchronized (requests) {
            int mxid = message.getHeader().getTransactionId();

            PendingRequest rh = requests.get(Pair.with(message.getHeader().getModuleId(), message.getHeader().getTransactionId()));

            if (rh != null) {
                // Message belongs to former request
                if (message instanceof FenceMessage) {
                    // check for finished execution
                    // TODO real check
                    rh.result.signalIsDone((FenceMessage) message);
                    rh.lock.release();
                    logger.info("Data completes request (" + message.toString() + ").");
                } else {
                    // add to result
                    try {
                        rh.result.addResultMessage(message);
                    } catch (IllegalStateException ise) {
                        logger.error("FIXME", ise);
                    }
                    logger.info("Message adds to running request (" + message.toString() + ").");
                }
            } else {
                // Random message from backend
                if (message instanceof HelloMessage) {
                    // just relay it to the shim
                    logger.info("Received HelloMessage from backend, relaying to shim...");
                    connector.SendData(message.toByteRepresentation(), Constants.SHIM);
                    return;
                } else if (message instanceof ModuleAnnouncementMessage) {
                    // remember backend
                    if (!backendIds.stream().anyMatch(a -> a.equals(backendId))) {
                        backendIds.add(backendId);
                    }
                    // get new module ID
                    ModuleAnnouncementMessage mam = (ModuleAnnouncementMessage) message;
                    String moduleName = mam.getModuleName();
                    logger.info("Received ModuleAnnouncement for module '" + moduleName + "' from backend '" + backendId + "'. Calculating ID...");
                    int moduleId = random.nextInt(1000); // for easier typing in the emulator, restrict to smaller numbers
                    while (moduleToNameMappings.keySet().contains(moduleId) || moduleId < 1) {
                        moduleId = random.nextInt(1000);
                    }

                    if (moduleToNameMappings.values().contains(moduleName)) {
                        int oldid = getModuleId(moduleName);
                        logger.warn("Module with name %s already exists (id: %d), using newer module", moduleName, oldid);
                        moduleToNameMappings.put(oldid, "old_" + moduleName);
                        markModuleAllOutstandingRequestsAsFinished(oldid);
                    }

                    moduleToNameMappings.put(moduleId, moduleName);
                    moduleToBackendMappings.put(moduleId, backendId);
                    // send acknowledge back
                    ModuleAcknowledgeMessage ack = new ModuleAcknowledgeMessage();
                    ack.setModuleName(moduleName);
                    ack.setHeader(NetIPUtils.StubHeaderFromPayload(ack.getPayload()));
                    ack.getHeader().setMessageType(MessageType.MODULE_ACKNOWLEDGE);
                    ack.getHeader().setModuleId(moduleId);
                    connector.SendData(ack.toByteRepresentation(), backendId);
                    logger.info("Mapped module '" + moduleName + "' to id '" + moduleId + "' and sent ModuleAcknowledgeMessage to backend '" + backendId + "'.");
                } else if (message instanceof ManagementMessage) {
                    logger.info("Received unrequested ManagementMessage: '" + message.toString() + "'. Relaying to shim.");
                    connector.SendData(message.toByteRepresentation(), Constants.SHIM);
                } else if (message instanceof FenceMessage) {
                    if (((FenceMessage) message).getHeader().getTransactionId()==0) {
                        logger.debug("FenceMessage with xid==0 makes no sense '" + message.toString() + "'. Dropping message");
                    } else{
                        logger.error("Received unrequested FenceMessage: '" + message.toString() + "'. Dropping message");
                    }
                } else {
                    logger.info("Received unrequested Message: '" + message.toString() + "'. Relaying to shim.");
                    try {
                        listenerLock.acquire();
                        // Notify listeners and send to shim
                        for (IBackendMessageListener listener : backendMessageListeners) {
                            pool.submit(() -> listener.OnBackendMessage(message, backendId));
                        }
                        connector.SendData(message.toByteRepresentation(), Constants.SHIM); // TODO make shim name a constant
                    } catch (InterruptedException e) {
                        logger.error("", e);
                    } finally {
                        listenerLock.release();
                    }
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
