package eu.netide.core.api.netip;

import java.nio.ByteBuffer;

/**
 * Created by timvi on 06.08.2015.
 */
public class MessageHeader {
    private ProtocolVersion protocolVersion;
    private MessageType messageType;
    private short payloadLength;
    private int transactionId;
    private long datapathId;

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public short getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(short payloadLength) {
        this.payloadLength = payloadLength;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public long getDatapathId() {
        return datapathId;
    }

    public void setDatapathId(long datapathId) {
        this.datapathId = datapathId;
    }

    public byte[] toByteRepresentation() {
        byte[] bytes = new byte[16];
        bytes[0] = protocolVersion.getValue();
        bytes[1] = messageType.getValue();
        System.arraycopy(ByteBuffer.allocate(2).putShort(payloadLength).array(), 0, bytes, 2, 2);
        System.arraycopy(ByteBuffer.allocate(4).putInt(transactionId).array(), 0, bytes, 4, 4);
        System.arraycopy(ByteBuffer.allocate(8).putInt(transactionId).array(), 0, bytes, 8, 8);
        return bytes;
    }
}
