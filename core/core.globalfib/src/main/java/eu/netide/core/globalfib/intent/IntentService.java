package eu.netide.core.globalfib.intent;

import eu.netide.core.globalfib.FlowModEntry;

/**
 * Created by msp on 9/27/16.
 */
public interface IntentService {
    void process(FlowModEntry flowModEntry);
}
