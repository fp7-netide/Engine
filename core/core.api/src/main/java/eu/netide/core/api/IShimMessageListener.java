package eu.netide.core.api;

import eu.netide.lib.netip.Message;

/**
 * Created by timvi on 08.07.2015.
 */
public interface IShimMessageListener {
    void OnShimMessage(Message message);
}
