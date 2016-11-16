package eu.netide.core.globalfib.intent;

import java.util.Set;

/**
 * Created by msp on 9/27/16.
 */
public interface IntentService {
    void process(FlowModEntry flowModEntry);

    Set<Intent> getIntents();
}
