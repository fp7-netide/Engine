package eu.netide.core.globalfib;

import eu.netide.core.api.IShimMessageListener;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.OFPacketIn;

public class FIBManager implements IShimMessageListener{
    private final OFMessageReader<OFMessage> reader;
    private final GlobalFIB gfib;

    public FIBManager()
    {
        reader = OFFactories.getGenericReader();
        gfib = new GlobalFIB();

    }

    @Override
    public void OnShimMessage(Message message, String originId) {
        System.out.println("FIBManager received message from shim: " + new String(message.getPayload()));
        if (message.getHeader().getMessageType() == MessageType.OPENFLOW) {
            // OpenFlow Message

            // Our API is broken

            ChannelBuffer bb = ChannelBuffers.copiedBuffer(message.getPayload());
            try {
                OFMessage ofmessage = reader.readFrom(bb);
                long datapathId = message.getHeader().getDatapathId();
                if (ofmessage instanceof OFFlowAdd) {
                    OFFlowAdd ofFlowAdd = (OFFlowAdd) ofmessage;

                    gfib.addFlow(datapathId, ofFlowAdd);


                } if (ofmessage instanceof OFPacketIn) {
                    gfib.handlePacketIn((OFPacketIn) ofmessage, datapathId);
                }
            } catch (OFParseError ofParseError) {
                ofParseError.printStackTrace();
            }

        }
    }

}
