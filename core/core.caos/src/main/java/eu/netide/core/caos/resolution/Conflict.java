package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.Message;

/**
 * Created by timvi on 31.08.2015.
 */
public abstract class Conflict {
    protected Message message1;
    protected Message message2;

    public Message getMessage1() {
        return message1;
    }

    public Message getMessage2() {
        return message2;
    }
}
