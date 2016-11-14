package eu.netide.core.globalfib;

import eu.netide.core.api.IFIBManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.caos.ICompositionManager;
import eu.netide.core.globalfib.intent.Intent;
import eu.netide.core.globalfib.intent.IntentService;
import eu.netide.core.globalfib.topology.TopologySpecification;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.OpenFlowMessage;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.util.HashSet;
import java.util.List;
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
    public void OnShimMessage(Message message, String originId) {
        log.info("FIBManager received message from shim: " + message.getHeader().toString());

        List<Message> backendResults = compositionManager.processShimMessage(message, originId);
        for (Message result : backendResults) {
            handleResult(result);
        }

        if (message.getHeader().getMessageType() == MessageType.OPENFLOW) {
            OpenFlowMessage ofMessage = (OpenFlowMessage) message;
            if (ofMessage.getOfMessage().getType() == OFType.ECHO_REQUEST) {
                return;
            }

            ChannelBuffer bb = ChannelBuffers.copiedBuffer(message.getPayload());
            try {
                OFMessage ofmessage = reader.readFrom(bb);
                long datapathId = message.getHeader().getDatapathId();
                if (ofmessage instanceof OFPacketIn) {
                    globalFIB.handlePacketIn((OFPacketIn) ofmessage, datapathId);
                }
            } catch (OFParseError ofParseError) {
                ofParseError.printStackTrace();
            }
        }
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
                globalFIB.addFlowMod(ofMessage);
            }
        }
        log.info("Relaying message to shim.");
        shimManager.sendMessage(message);
    }

    @Override
    public Set<OFFlowMod> getFlowMods() {
        Set<OFFlowMod> flowMods = new HashSet<>();
        for(FlowModEntry flowModEntry : globalFIB.getFlowModEntries()) {
            flowMods.add(flowModEntry.getFlowMod());
        }
        return flowMods;
    }

    @Override
    public Set<String> getIntentStrings() {
        Set<String> result = new HashSet<>();
        for (Intent intent: globalFIB.getIntents()) {
            result.add(intent.toString());
        }

        return result;
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

        TopologySpecification topologySpecification = TopologySpecification.topologySpecification(topologySpecificationXML);
        globalFIB.setTopologySpecification(topologySpecification);
    }

    public void bindShimManager(IShimManager shimManager) {
        this.shimManager = shimManager;
    }
}