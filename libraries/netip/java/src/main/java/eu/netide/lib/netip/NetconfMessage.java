package eu.netide.lib.netip;

/**
 * Class representing a message of type NETCONF.
 * Note that this only serves as a convenience class - if the MessageType is manipulated, the class will not recognize that.
 */
public class NetconfMessage extends Message {

    /**
     * Instantiates a new Netconf message.
     */
    public NetconfMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.NETCONF);
    }

    /**
     * Sets payload.
     *
     * @param payload the payload
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public byte[] getPayload() {
        return this.payload;
    }
}
