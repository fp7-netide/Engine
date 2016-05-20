package eu.netide.core.caos;

import eu.netide.lib.netip.Message;

import java.util.List;

/**
 * Created by timvi on 25.06.2015.
 */
public interface ICompositionManager {
    List<Message> processShimMessage(Message message, String originId);
}
