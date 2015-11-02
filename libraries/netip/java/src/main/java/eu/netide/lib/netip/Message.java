package eu.netide.lib.netip;

import java.util.Arrays;
import java.util.IllegalFormatException;

/**
 * Class representing a simple NetIP message. This also is the base class for concrete message classes.
 */
public class Message {
    /**
     * The header.
     */
    protected MessageHeader header;
    /**
     * The payload.
     */
    protected byte[] payload;

    /**
     * Creates a new instance of the Message class.
     *
     * @param header  The header to use.
     * @param payload The payload.
     */
    public Message(MessageHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    /**
     * Gets the header.
     *
     * @return the header.
     */
    public MessageHeader getHeader() {
        return this.header;
    }

    /**
     * Sets the header.
     *
     * @param header The new header.
     */
    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    /**
     * Gets the current payload as bytes.
     *
     * @return the payload
     * @implNote This method has to ensure that the returned payload reflects the current state of any convenience fields!
     */
    public byte[] getPayload() {
        return this.payload;
    }

    /**
     * Returns the message's byte representation, including the header.
     *
     * @return The byte representation.
     */
    public byte[] toByteRepresentation() {
        byte[] payload = getPayload();

        byte[] bytes = new byte[MessageHeader.HEADER_BYTES + payload.length];
        header.setPayloadLength((short) payload.length);
        System.arraycopy(header.toByteRepresentation(), 0, bytes, 0, MessageHeader.HEADER_BYTES);
        System.arraycopy(payload, 0, bytes, MessageHeader.HEADER_BYTES, payload.length);
        if (header.getMessageType() == MessageType.OPENFLOW && payload.length < 8 )
            throw  new InvalidNetIdeMessage("OpenFlow messages need to be at least 8 bytes long");

        return bytes;
    }

    @Override
    public String toString() {
        return "Message [Header=" + header.toString() + ",Payload=" + Arrays.toString(getPayload()) + "]";
    }
}
