package eu.netide.lib.netip;

/**
 * Class representing a message of type MANAGEMENT.
 * Note that this only serves as a convenience class - if the MessageType is manipulated, the class will not recognize that.
 */
public class ManagementMessage extends Message {

    private String payloadString;

    public ManagementMessage() {
        super(new MessageHeader(), new byte[0]);
        header.setMessageType(MessageType.MANAGEMENT);
    }

    public String getPayloadString() {
        return payloadString;
    }

    public void setPayloadString(String payloadString) {
        this.payloadString = payloadString;
    }

    @Override
    public byte[] getPayload() {
        return payloadString.getBytes();
    }
}
