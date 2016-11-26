package eu.netide.core.globalfib;

import eu.netide.core.api.*;
import eu.netide.core.globalfib.intent.FlowModEntry;
import eu.netide.core.globalfib.intent.Intent;
import eu.netide.core.globalfib.topology.TopologySpecification;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.OpenFlowMessage;
import org.javatuples.Pair;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FIBManager implements IShimMessageListener, IFIBManager {
    private final OFMessageReader<OFMessage> reader;

    private static final Logger log = LoggerFactory.getLogger(FIBManager.class);

    private IGlobalFIB globalFIB;

    private ICompositionManager compositionManager;

    private IShimManager shimManager;

    private String topologySpecificationXML;

    public FIBManager() {
        reader = OFFactories.getGenericReader();
    }

    public void Start() {
        log.info("FIBManager started.");
    }

    public void Stop() {
        log.info("FIBManager stopped.");
    }

    @Override
    public MessageHandlingResult OnShimMessage(Message message, String originId) {
        log.info("FIBManager received message from shim: " + message.getHeader().toString());

        List<Message> backendResults = compositionManager.processShimMessage(message, originId);
        for (Message result : backendResults) {
            handleResult(result);
        }

        if (message.getHeader().getMessageType() == MessageType.OPENFLOW) {
            OpenFlowMessage ofMessage = (OpenFlowMessage) message;
            if (ofMessage.getOfMessage().getType() == OFType.ECHO_REQUEST) {
                return MessageHandlingResult.RESULT_PASS;

            }

            ChannelBuffer bb = ChannelBuffers.copiedBuffer(message.getPayload());
            try {
                OFMessage ofmessage = reader.readFrom(bb);
                long datapathId = message.getHeader().getDatapathId();
                if (ofmessage instanceof OFPacketIn) {
                    globalFIB.handlePacketIn((OFPacketIn) ofmessage, datapathId);

                    // TODO: Change if method above actually handles the packet
                    //return MessageHandlingResult.RESULT_PROCESSED;
                    return MessageHandlingResult.RESULT_PASS;


                }
            } catch (OFParseError ofParseError) {
                ofParseError.printStackTrace();
            }
        }
        if (backendResults == null)
            return MessageHandlingResult.RESULT_PASS;
        else
            return MessageHandlingResult.RESULT_PROCESSED;
    }

    @Override
    public void OnOutgoingShimMessage(Message message) {
    }

    @Override
    public void OnUnhandeldShimMessage(Message message, String originId) {
        compositionManager.processUnhandledShimMessage(message, originId);

    }

    /**
     * Handles answers from the CompositionManager.
     *
     * @param message Message from the CompositionManager.
     */
    public void handleResult(Message message) {
        if (message.getHeader().getMessageType() == MessageType.OPENFLOW) {
            OpenFlowMessage ofMessage = (OpenFlowMessage) message;
            if (ofMessage.getOfMessage().getType() == OFType.FLOW_MOD) {
                try {
                    globalFIB.addFlowMod(ofMessage);
                } catch (Exception e) {
                    log.error("GlobalFIB failed to add FlowMod", e);
                }
            }
        }
        log.info("Relaying message to shim: {}", message);
        shimManager.sendMessage(message);
    }

    @Override
    public Set<IFlowModEntry> getFlowModEntries() {
        return globalFIB.getFlowModEntries();
    }

    @Override
    public Set<IIntent> getIntents() {
        return globalFIB.getIntents();
    }

    public void setShimManager(IShimManager shimManager) {
        this.shimManager = shimManager;
    }

    public void setGlobalFIB(IGlobalFIB globalFIB) {
        this.globalFIB = globalFIB;
    }

    public void setCompositionManager(ICompositionManager compositionManager) {
        this.compositionManager = compositionManager;
    }

    public void setTopologySpecificationXML(String topologySpecificationXML) throws JAXBException {
        this.topologySpecificationXML = topologySpecificationXML;
        if (this.topologySpecificationXML.isEmpty()) {
            return;
        }
        log.info("Setting Topology Specification: " + this.topologySpecificationXML);

        TopologySpecification topologySpecification = TopologySpecification.topologySpecification(topologySpecificationXML);
        globalFIB.setTopologySpecification(topologySpecification);
    }
}