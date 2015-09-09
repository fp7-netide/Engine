package eu.netide.lib.netip;

/**
 * Class representing a message of type OPFLEX.
 * Note that this only serves as a convenience class - if the MessageType is manipulated, the class will not recognize that.
 */
public class OpFlexMessage extends Message {

    /**
     * Instantiates a new Op flex message.
     */
    public OpFlexMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.OPFLEX);
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
