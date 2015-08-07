package eu.netide.core.api;

import eu.netide.core.api.netip.Message;

/**
 * Created by timvi on 08.07.2015.
 */
public interface IShimManager {
    IShimConnector getConnector();

    boolean sendMessage(Message message);
}
