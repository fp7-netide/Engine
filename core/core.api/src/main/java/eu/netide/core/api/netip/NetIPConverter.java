package eu.netide.core.api.netip;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by timvi on 06.08.2015.
 */
public abstract class NetIPConverter {

    public static Message parseConcreteMessage(byte[] data) {
        return NetIPUtils.ConcretizeMessage(parseRawMessage(data));
    }


    public static Message parseRawMessage(byte[] data) {
        Message message = new Message();
        message.setHeader(parseHeader(Arrays.copyOfRange(data, 0, 15)));
        message.setPayload(Arrays.copyOfRange(data, 16, data.length - 1));
        return message;
    }

    public static MessageHeader parseHeader(byte[] data) {
        if (data.length != 32)
            throw new IllegalArgumentException("Header byte size has to be 32");
        MessageHeader header = new MessageHeader();
        header.setProtocolVersion(ProtocolVersion.parse(data[0]));
        header.setMessageType(MessageType.parse(data[1]));
        header.setPayloadLength(ByteBuffer.wrap(data, 2, 2).getShort());
        header.setTransactionId(ByteBuffer.wrap(data, 4, 4).getInt());
        header.setDatapathId(ByteBuffer.wrap(data, 8, 8).getLong());
        return header;
    }
}
