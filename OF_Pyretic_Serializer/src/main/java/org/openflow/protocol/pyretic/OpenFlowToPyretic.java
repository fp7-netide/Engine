package org.openflow.protocol.pyretic;


import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFStatisticsRequest;

public interface OpenFlowToPyretic {

	public  String serializeMessage(long switchID, OFStatisticsRequest statsRequest);
	public  String serializeMessage(long switchID, OFPacketOut packetOut);
	public  String serializeMessage(long switchID, OFFlowMod flowMod);
}
