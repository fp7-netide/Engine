package eu.netide.core.api;

import eu.netide.lib.netip.Message;

import java.util.List;

/**
 * Created by timvi on 25.06.2015.
 */
public interface ICompositionManager {
    /**
     *
     * @param message
     * @param originId
     * @return A list if the message was processed. Null if the message was
     *  ignored
     */
    List<Message> processShimMessage(Message message, String originId);

    void processUnhandledShimMessage(Message message, String originId);
}
