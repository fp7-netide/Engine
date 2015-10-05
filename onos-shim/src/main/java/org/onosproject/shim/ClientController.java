package org.onosproject.shim;

import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onosproject.net.Device;
import org.onosproject.shim.message.NetIDEHeader;
import org.onosproject.shim.message.NetIDEMessage;
import org.onosproject.shim.message.NetIDEProtocol;
import org.onosproject.shim.message.NetIDEType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;

public class ClientController {
	private final Logger log = getLogger(getClass());
	
	TCPClient tcpClient;
	List<NetIDEProtocol> protocolList;
	private static int XID = 0;

	public ClientController(TCPClient client, List<NetIDEProtocol> proto) {
		this.tcpClient = client;
		this.protocolList = proto;
	}

	public TCPClient getTcpClient() {
		return tcpClient;
	}

	public void setTcpClient(TCPClient tcpClient) {
		this.tcpClient = tcpClient;
	}

	public List<NetIDEProtocol> getProtocols() {
		return protocolList;
	}
	
	private int getNextXid() {
		return XID++;
	}

	public void setProtocols(List<NetIDEProtocol> protocol) {
		this.protocolList = protocol;
	}
	
	public NetIDEProtocol getPreferredProtocol() {
		return this.protocolList.get(0);
	}
	
	private short getOFLenght(byte[] payload) {
		log.info(">>>> Lunghezza =" + ByteBuffer.wrap(payload, 2, 3).getShort());
		return ByteBuffer.wrap(payload, 2, 3).getShort();
	}
	
	private byte[] resizePayload(byte[] payload, short lenght) {
		return ByteBuffer.wrap(payload, 0, lenght).array();
	}

	public NetIDEMessage createOF10Message(ByteBuffer byteBuffer, Device dev) {
		NetIDEMessage msg = new NetIDEMessage();
		NetIDEHeader header = new NetIDEHeader();
		header.setDatapathId(dev.chassisId().value());
		header.setNetIDEVersion((byte)0x01);
		header.setType(NetIDEType.NETIDE_OPENFLOW.getNetIDETypeId());
		header.setXid(getNextXid());
		byte [] payload = byteBuffer.array();
		header.setLenght(getOFLenght(payload));
		msg.setHeader(header);
		msg.setPayload(resizePayload(payload, header.getLenght()), header.getLenght());
		return msg;
	}
	
	public NetIDEMessage EncodeOF10Message(OFMessage ofmsg, Device dev) {
		NetIDEMessage msg = new NetIDEMessage();
		NetIDEHeader header = new NetIDEHeader();
		header.setDatapathId(dev.chassisId().value());
		header.setNetIDEVersion((byte)0x01);
		header.setType(NetIDEType.NETIDE_OPENFLOW.getNetIDETypeId());
		header.setXid(getNextXid());
		ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
		ofmsg.writeTo(buf);
		byte [] payload = buf.array();
		header.setLenght(getOFLenght(payload));
		msg.setHeader(header);
		msg.setPayload(resizePayload(payload, header.getLenght()), header.getLenght());
		return msg;
	}
}
