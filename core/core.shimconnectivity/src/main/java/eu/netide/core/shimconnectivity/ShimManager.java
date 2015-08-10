package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IConnectorListener;
import eu.netide.core.api.IShimConnector;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.NetIPConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by timvi on 08.07.2015.
 */
public class ShimManager implements IShimManager, IConnectorListener {

    private IShimConnector connector;
    private List<IShimMessageListener> shimMessageListeners;
    private Semaphore listenerLock = new Semaphore(1);

    public void Start() {
        System.out.println("ShimManager started.");
    }

    public void Stop() {
        System.out.println("ShimManager stopped.");
    }

    @Override
    public boolean sendMessage(Message message) {
        return connector.SendData(message.toByteRepresentation());
    }

    @Override
    public void OnDataReceived(byte[] data) {
        Message message = NetIPConverter.parseConcreteMessage(data);
        try {
            listenerLock.acquire();
            for (IShimMessageListener listener : shimMessageListeners) {
                listener.OnShimMessage(message);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            listenerLock.release();
        }
    }

    public void setConnector(IShimConnector connector) {
        this.connector = connector;
        connector.RegisterListener(this);
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
}
