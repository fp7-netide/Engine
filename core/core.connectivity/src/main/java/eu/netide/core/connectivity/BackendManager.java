package eu.netide.core.connectivity;

import eu.netide.core.api.IBackendConnector;
import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IBackendMessageListener;
import eu.netide.core.api.IConnectorListener;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by timvi on 11.08.2015.
 */
public class BackendManager implements IBackendManager, IConnectorListener {
    private IBackendConnector connector;
    private List<IBackendMessageListener> backendMessageListeners;
    private Semaphore listenerLock = new Semaphore(1);
    private List<String> backendIds = new ArrayList<>();

    public void Start() {
        System.out.println("BackendManager started.");
    }

    public void Stop() {
        System.out.println("BackendManager stopped.");
    }

    @Override
    public boolean sendMessage(Message message, String backendId) {
        return connector.SendData(message.toByteRepresentation(), backendId);
    }

    @Override
    public List<String> getBackendIds() {
        return backendIds;
    }

    @Override
    public void OnDataReceived(byte[] data, String backendId) {
        Message message = NetIPConverter.parseConcreteMessage(data);
        try {
            listenerLock.acquire();
            for (IBackendMessageListener listener : backendMessageListeners) {
                listener.OnBackendMessage(message, backendId);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            listenerLock.release();
        }
    }

    public void setConnector(IBackendConnector connector) {
        this.connector = connector;
        connector.RegisterBackendListener(this);
    }

    public IBackendConnector getConnector() {
        return connector;
    }

    public List<IBackendMessageListener> getBackendMessageListeners() {
        return backendMessageListeners;
    }

    public void setBackendMessageListeners(List<IBackendMessageListener> backendMessageListeners) throws InterruptedException {
        listenerLock.acquire();
        this.backendMessageListeners = backendMessageListeners == null ? new ArrayList<>() : backendMessageListeners;
        listenerLock.release();
    }
}
