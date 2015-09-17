package org.onosproject.shim.message;

import static org.slf4j.LoggerFactory.getLogger;

import java.awt.List;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;

public class NetIDEMessage {
	private final Logger log = getLogger(getClass());
	
	NetIDEHeader header;
	byte[] payload;
	NetIDEHello hello;
	
	public NetIDEMessage() {
		this.header = new NetIDEHeader();
		this.payload = null;
		this.hello = null;
	}
	
	public NetIDEMessage(NetIDEHeader header, byte[] payload) {
		this.header = header;
		this.payload = payload;
	}

	public NetIDEHeader getHeader() {
		return header;
	}

	public void setHeader(NetIDEHeader header) {
		this.header = header;
	}

	public byte[] getPayload() {
		return payload;
	}
	
	public int getPayloadLeght() {
		return this.payload.length;
	}

	public void setPayload(byte[] payload, short lenght) {
		this.payload = new byte[lenght];
		System.arraycopy( payload, 0, this.payload, 0, lenght);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(this.payload.length * 2);
		for(byte b: this.payload)
		      sb.append(String.format("%02x", b & 0xff));
		return this.header.toString() + " payload: " + sb.toString();
	}
	
	public void setHello (NetIDEHello hello) {
		this.hello = hello;
	}
	
	public NetIDEHello getHello() {
		return this.hello;
	}

	public byte[] toByteArray() {
		byte[] msg = new byte[this.header.getHeaderLenght() + getPayloadLeght()]; 
		msg[0] = this.header.getNetIDEVersion();
		msg[1] = this.header.getType().getNetIDETypeId();
		byte[] lenght = ByteBuffer.allocate(2).putShort(this.header.getLenght()).array();
		byte[] xid = ByteBuffer.allocate(4).putInt(this.header.getXid()).array();
		byte[] dp = ByteBuffer.allocate(8).putLong(this.header.getDatapathId()).array();
		System.arraycopy(lenght,0,msg,2,2);
		System.arraycopy(xid,0,msg,4,4);
		System.arraycopy(dp,0,msg,8,8);
		System.arraycopy(this.payload,0,msg,16,this.payload.length);
		log.info("ecco quello che mando " + toString());
		return msg;
	}
	
}
