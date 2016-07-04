package eu.netide.core.globalfib;

import eu.netide.core.api.IFIBManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.api.ICompositionManager;
import eu.netide.core.api.MessageHandlingResult;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.OpenFlowMessage;
import org.apache.felix.scr.annotations.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onosproject.net.flow.FlowEntry;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component(immediate=true)
@Service
public class FIBManager implements IFIBManager, IShimMessageListener {
    private final OFMessageReader<OFMessage> reader;
    private final GlobalFIB globalFIB;

    private static final Logger log = LoggerFactory.getLogger(FIBManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ICompositionManager compositionManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IShimManager shimManager;

    //@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    //private TopologyService topologyService;

    public FIBManager()
    {
        reader = OFFactories.getGenericReader();
        globalFIB = new GlobalFIB();
    }

    @Activate
    public void Start()
    {
        log.info("FIBManager started.");
    }

    @Deactivate
    public void Stop()
    {
        log.info("FIBManager stopped.");
    }

    @Override
    public MessageHandlingResult OnShimMessage(Message message, String originId) {
        log.info("FIBManager received message from shim: " + message.getHeader().toString());

        List<Message> backendResults = compositionManager.processShimMessage(message, originId);
        for (Message result: backendResults) {
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
                if (ofmessage instanceof OFFlowAdd) {
                    OFFlowAdd ofFlowAdd = (OFFlowAdd) ofmessage;

                    //globalFIB.addFlowMod(ofFlowAdd, datapathId);
                } if (ofmessage instanceof OFPacketIn) {
                    globalFIB.handlePacketIn((OFPacketIn) ofmessage, datapathId);

                    // TODO: Change if method above actually handles the packet
                    //return MessageHandlingResult.RESULT_PROCESSED;
                    return MessageHandlingResult.RESULT_PASS;


                }
            } catch (OFParseError ofParseError) {
                ofParseError.printStackTrace();
            }
        }
        return MessageHandlingResult.RESULT_PASS;
    }

    @Override
    public void OnOutgoingShimMessage(Message message) {
    }

    public void handleResult(Message message) {
        if (message.getHeader().getMessageType() == MessageType.OPENFLOW) {
            OpenFlowMessage ofMessage = (OpenFlowMessage) message;
            if (ofMessage.getOfMessage().getType() == OFType.FLOW_MOD) {
                globalFIB.addFlowMod(ofMessage);
            }
        }
        log.info("Relaying message to shim: {}", message);
        shimManager.sendMessage(message);
    }

    // Used in Unit tests
    public void bindShimManager(IShimManager shimManager) {
        this.shimManager = shimManager;
    }

    @Override
    public List<FlowEntry> getFlowMods() {
        return globalFIB.getFlowEntries();
    }
}
