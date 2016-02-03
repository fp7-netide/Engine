package eu.netide.core.api;

import eu.netide.lib.netip.ManagementMessage;

/**
 * Interface for ManagementMessage listeners.
 * <p>
 * Created by timvi on 19.08.2015.
 */
public interface IManagementMessageListener {
    void OnManagementMessage(ManagementMessage message);
}
