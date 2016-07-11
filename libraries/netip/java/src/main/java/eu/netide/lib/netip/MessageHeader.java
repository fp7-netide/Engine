package eu.netide.lib.netip;

import java.nio.ByteBuffer;

/**
 * Class representing a NetIP header.
 */
public class MessageHeader implements Cloneable {

    /**
     * The length of a header in bytes.
     */
    public static final int HEADER_BYTES = 20;

    private NetIDEProtocolVersion netIDEProtocolVersion;
    private MessageType messageType;
    private short payloadLength;
    private int transactionId;
    private int moduleId;
    private long datapathId;

    public MessageHeader() {
        this.netIDEProtocolVersion = NetIDEProtocolVersion.VERSION_1_3;
    }



    /**
     * Gets net iDE protocol version.
     *
     * @return the net iDE protocol version
     */
    public NetIDEProtocolVersion getNetIDEProtocolVersion() {
        return netIDEProtocolVersion;
    }

    /**
     * Sets net iDE protocol version.
     *
     * @param netIDEProtocolVersion the net iDE protocol version
     */
    public void setNetIDEProtocolVersion(NetIDEProtocolVersion netIDEProtocolVersion) {
        this.netIDEProtocolVersion = netIDEProtocolVersion;
    }

    /**
     * Gets message type.
     *
     * @return the message type
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Sets message type.
     *
     * @param messageType the message type
     */
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Gets payload length.
     *
     * @return the payload length
     */
    public short getPayloadLength() {
        return payloadLength;
    }

    /**
     * Sets payload length.
     *
     * @param payloadLength the payload length
     */
    public void setPayloadLength(short payloadLength) {
        this.payloadLength = payloadLength;
    }

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public int getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets module id.
     *
     * @return the module id
     */
    public int getModuleId() {
        return moduleId;
    }

    /**
     * Sets module id.
     *
     * @param moduleId the module id
     */
    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Gets datapath id.
     *
     * @return the datapath id
     */
    public long getDatapathId() {
        return datapathId;
    }

    /**
     * Sets datapath id.
     *
     * @param datapathId the datapath id
     */
    public void setDatapathId(long datapathId) {
        this.datapathId = datapathId;
    }

    /**
     * Returns the header's byte representation.
     *
     * @return The byte representation.
     */
    public byte[] toByteRepresentation() {
        byte[] bytes = new byte[HEADER_BYTES];
        bytes[0] = netIDEProtocolVersion.getValue();
        bytes[1] = messageType.getValue();
        System.arraycopy(ByteBuffer.allocate(2).putShort(payloadLength).array(), 0, bytes, 2, 2);
        System.arraycopy(ByteBuffer.allocate(4).putInt(transactionId).array(), 0, bytes, 4, 4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(moduleId).array(), 0, bytes, 8, 4);
        System.arraycopy(ByteBuffer.allocate(8).putLong(datapathId).array(), 0, bytes, 12, 8);
        return bytes;
    }

    @Override
    public String toString() {
        return "MessageHeader [Version=" + netIDEProtocolVersion.name() + ",Type=" + messageType.name() + ",Length=" + payloadLength + ",ModuleId=" + moduleId + ",TransactionId=" + transactionId + ",DatapathId=" + datapathId + "]";
    }
}
