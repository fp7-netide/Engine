package eu.netide.core.api;

import eu.netide.core.api.netip.Message;

/**
 * Created by timvi on 25.06.2015.
 */
public interface IShimConnector {

    void SendMessage(Message message);
}
