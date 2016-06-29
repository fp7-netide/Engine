package eu.netide.core.connectivity;

import eu.netide.core.api.*;
import eu.netide.lib.netip.HelloMessage;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by timvi on 08.07.2015.
 */
public class ShimManager implements IShimManager, IConnectorListener {

    private static final Logger logger = LoggerFactory.getLogger(ShimManager.class);

    private IShimConnector connector;
    private List<IShimMessageListener> shimMessageListeners;
    private Semaphore listenerLock = new Semaphore(1);
    private ExecutorService pool = Executors.newCachedThreadPool();
    private IBackendManager backendManager;
    private long mLastMessageTime=0;

    public void Start() {
        logger.info("ShimManager started.");
    }

    public void Stop() {
        logger.info("ShimManager stopped.");
    }

    @Override
    public boolean sendMessage(Message message) {
        try {
            listenerLock.acquire();
            for (IShimMessageListener listener : shimMessageListeners) {
                pool.submit(() -> listener.OnOutgoingShimMessage(message));
            }
        } catch (InterruptedException e) {
            logger.error("", e);
        } finally {
            listenerLock.release();
        }
        return connector.SendData(message.toByteRepresentation());
    }

    @Override
    public long getLastMessageTime() {
        return mLastMessageTime;
    }

    @Override
    public void OnDataReceived(byte[] data, String originId) {
        mLastMessageTime = System.currentTimeMillis();
        Message message;
        try {
            message = NetIPConverter.parseConcreteMessage(data);
        } catch (Exception ex) {
            logger.error("Unable to parse received data from '" + originId + "' (" + Arrays.toString(data) + ").", ex);
            return;
        }
        if (message instanceof HelloMessage) {
            // relay back to the correct backend
            logger.info("Received HelloMessage, relaying to backend '" + backendManager.getBackend(message.getHeader().getModuleId()) + "'.");
            backendManager.sendMessage(message);
            return;
        }
        try {
            listenerLock.acquire();
            IShimMessageListener[] shimListenerCopy = shimMessageListeners.toArray(new IShimMessageListener[shimMessageListeners.size()]);
            listenerLock.release();

            pool.submit(() -> {
                MessageHandlingResult retTotal =MessageHandlingResult.RESULT_PASS;
                for (IShimMessageListener listener : shimListenerCopy) {
                    MessageHandlingResult ret = listener.OnShimMessage(message, originId);
                    if (ret != MessageHandlingResult.RESULT_PASS)
                        retTotal=ret;
                }
                if (retTotal == MessageHandlingResult.RESULT_PASS) {
                    for (IShimMessageListener listener : shimListenerCopy) {
                        listener.OnUnhandeldShimMessage(message, originId);
                    }
                }
            });
        } catch (InterruptedException e) {
            logger.error("", e);
        } finally {
        }
    }

    public void setConnector(IShimConnector connector) {
        this.connector = connector;
        connector.RegisterShimListener(this);
    }

    public IShimConnector getConnector() {
        return connector;
    }

    public List<IShimMessageListener> getShimMessageListeners() {
        return shimMessageListeners;
    }

    public void setShimMessageListeners(List<IShimMessageListener> shimMessageListeners) throws InterruptedException {
        listenerLock.acquire();
        this.shimMessageListeners = shimMessageListeners == null ? new ArrayList<>() : shimMessageListeners;
        listenerLock.release();
    }

    /**
     * Sets the backend manager.
     *
     * @param manager the manager
     */
    public void setBackendManager(IBackendManager manager) {
        backendManager = manager;
        logger.info("BackendManager set.");
    }

    /**
     * Gets the backend manager.
     *
     * @return the backend manager
     */
    public IBackendManager getBackendManager() {
        return backendManager;
    }
}
