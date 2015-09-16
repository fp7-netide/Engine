package org.opendaylight.netide.shim;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.opendaylight.openflowjava.protocol.impl.deserialization.action.AbstractActionDeserializer;
import org.opendaylight.openflowjava.protocol.impl.deserialization.action.OF10SetTpDstActionDeserializer;
import org.opendaylight.openflowjava.protocol.impl.util.MatchDeserializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev150203.actions.grouping.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInputBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;

import eu.netide.lib.netip.OpenFlowMessage;
import io.netty.buffer.Unpooled;


public class OFMessageTranslator {
	
	public static DataObject translate(OpenFlowMessage ofm){
		
		switch(ofm.getOfMessage().getType()){
			case FLOW_MOD:
				OFFlowMod ofmFlowMod = (OFFlowMod)ofm.getOfMessage();
				return flowModInputTranslator(ofmFlowMod);
			default:
				return null;
		}
		
	}
	
	public static FlowModInput flowModInputTranslator(OFFlowMod ofmFlowMod){
		
		//Deserialize Match
		ChannelBuffer buff = ChannelBuffers.dynamicBuffer();
		ofmFlowMod.getMatch().writeTo(buff);
		byte[] matchBytes = buff.array();
		MatchDeserializer matchDeserializer = new MatchDeserializer();
		Match match = matchDeserializer.deserialize(Unpooled.wrappedBuffer(matchBytes));
		
		//Deserielize Actions
		List<Action> actions = new ArrayList<Action>();
		
		for (OFAction ofAction : ofmFlowMod.getActions()){
			Action action = actionTranslator(ofAction);
			if (action != null)
				actions.add(action);
		}
		
		//Set builder
		FlowModInputBuilder builder = new FlowModInputBuilder();
		builder.setMatch(match);
		builder.setAction(actions);
		return builder.build();
	}
	
	public static Action actionTranslator(OFAction ofmAction){
		ChannelBuffer buff = ChannelBuffers.dynamicBuffer();
		ofmAction.writeTo(buff);
		byte[] actionBytes = buff.array();
		AbstractActionDeserializer deserializer = null;
		switch(ofmAction.getType()){
			case SET_TP_DST:
				deserializer = new OF10SetTpDstActionDeserializer();
				break;
			default:
				break;
		}
		
		if(deserializer != null){
			return deserializer.deserialize(Unpooled.wrappedBuffer(actionBytes));
		}
		return null;
	}

}
