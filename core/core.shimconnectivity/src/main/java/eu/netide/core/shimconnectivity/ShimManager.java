package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IShimConnector;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.api.netip.Message;

/**
 * Created by timvi on 08.07.2015.
 */
public class ShimManager implements IShimManager, IShimMessageListener {

    private IShimConnector _connector;

    public void Start() {
        System.out.println("ShimManager started.");
    }

    public void Stop() {
        System.out.println("ShimManager stopped.");
    }

    public void OnMessage(Message message) {
        System.out.println("Message from shim: " + new String(message.toByteRepresentation()));
    }

    public void setConnector(IShimConnector connector) {
        _connector = connector;
    }

    public IShimConnector getConnector() {
        return _connector;
    }
}
