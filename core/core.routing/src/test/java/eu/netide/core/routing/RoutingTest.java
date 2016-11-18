/**
 * Created by arne on 04.07.16.
 */

package eu.netide.core.routing;

import eu.netide.core.api.Constants;
import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IConnectorListener;
import eu.netide.core.api.IShimConnector;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.MessageHandlingResult;
import eu.netide.core.api.RequestResult;
import eu.netide.core.routing.OpenFlowRouting;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFEchoReply;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class RoutingTest {
    private OpenFlowRouting ofrouting;

    private Message shimOutMessage;
    private Message allBackendMessage;
    private Vector<Message> backEndMessages;
    private OFFactory fact;

    class FakeShim implements IShimManager {


        @Override
        public IShimConnector getConnector() {
            return null;
        }

        @Override
        public boolean sendMessage(Message message) {
            shimOutMessage = message;
            return true;
        }

        @Override
        public long getLastMessageTime() {
            return 0;
        }
    }

    class FakeBackend implements IBackendManager {

        @Override
        public boolean sendMessage(Message message) {
            backEndMessages.add(message);
            return true;
        }

        @Override
        public boolean sendMessageToAllModules(Message message) {
            allBackendMessage = message;
            return true;
        }

        @Override
        public RequestResult sendRequest(Message message) {
            return null;
        }

        @Override
        public Future<RequestResult> sendRequestAsync(Message message) {
            return null;
        }

        @Override
        public Stream<String> getBackendIds() {
            return null;
        }

        @Override
        public Stream<Integer> getModuleIds() {
            return null;
        }

        @Override
        public Stream<String> getModules() {
            return null;
        }

        @Override
        public String getModuleName(Integer moduleId) throws NoSuchElementException {
            return null;
        }

        @Override
        public String getBackend(Integer moduleId) throws NoSuchElementException {
            return null;
        }

        @Override
        public Long getLastMessageTime(Integer moduleId) {
            return null;
        }

        @Override
        public int getModuleId(String moduleName) throws NoSuchElementException {
            return 0;
        }

        @Override
        public void markModuleAllOutstandingRequestsAsFinished(int moduleId) {

        }

        @Override
        public void removeBackend(int id) {

        }
    }


    @BeforeTest
    void setupTests() {
        ofrouting = new OpenFlowRouting();
        ofrouting.setShim(new FakeShim());
        ofrouting.setBackend(new FakeBackend());
        backEndMessages = new Vector<>();
        fact = OFFactories.getFactory(OFVersion.OF_13);


    }

    @Test
    public void testUnrelatedMessage() {
        OFPacketIn.Builder b = fact.buildPacketIn();
        b.setData("Nonsense".getBytes());
        b.setReason(OFPacketInReason.NO_MATCH);
        OpenFlowMessage pktIn = new OpenFlowMessage();
        pktIn.setOfMessage(b.build());

        OFPacketOut.Builder bout = fact.buildPacketOut();
        bout.setData("Nonsense".getBytes());
        OpenFlowMessage pktOut = new OpenFlowMessage();
        pktOut.setOfMessage(bout.build());

        OpenFlowMessage echoReply = getEchoReply(111);
        OpenFlowMessage echoReq = getEchoReq(223);

        Assert.assertEquals(ofrouting.OnBackendMessage(pktIn, "FakeBackend"), MessageHandlingResult.RESULT_PASS);
        Assert.assertEquals(ofrouting.OnBackendMessage(pktOut, "FakeBackend"), MessageHandlingResult.RESULT_PASS);

        Assert.assertEquals(ofrouting.OnShimMessage(pktIn, Constants.SHIM), MessageHandlingResult.RESULT_PASS);
        Assert.assertEquals(ofrouting.OnShimMessage(pktOut, Constants.SHIM), MessageHandlingResult.RESULT_PASS);

        // Wrong direction
        Assert.assertEquals(ofrouting.OnShimMessage(echoReq, Constants.SHIM), MessageHandlingResult.RESULT_PASS);
        Assert.assertEquals(ofrouting.OnBackendMessage(echoReply, "FakeBackend"), MessageHandlingResult.RESULT_PASS);


    }

    @Test
    public void simpleRequestAndResponse() {
        int backendXid = 882;
        OpenFlowMessage ofMessage = getEchoReq(backendXid);
        Assert.assertEquals(ofrouting.OnBackendMessage(ofMessage, "FakeBackend"), MessageHandlingResult.RESULT_PROCESSED);

        Assert.assertNotNull(shimOutMessage);

        Assert.assertTrue(shimOutMessage instanceof OpenFlowMessage);

        OFMessage ofReqMessage = ((OpenFlowMessage) shimOutMessage).getOfMessage();
        long ofReqXid = ofReqMessage.getXid();
        Assert.assertTrue(Constants.FIRST_SHIM_XID <= ofReqXid && Constants.LAST_SHIM_XID >= ofReqXid);


        OpenFlowMessage ofFromShim = getEchoReply(ofReqXid);

        Assert.assertEquals(ofrouting.OnShimMessage(ofFromShim, Constants.SHIM), MessageHandlingResult.RESULT_PROCESSED);

        Assert.assertEquals(backEndMessages.size(), 1);

        Assert.assertEquals(((OpenFlowMessage) backEndMessages.get(0)).getOfMessage().getXid(), backendXid);

    }

    private OpenFlowMessage getEchoReply(long ofReqXid) {
        OFEchoReply.Builder bresp = fact.buildEchoReply();
        bresp.setXid(ofReqXid);
        bresp.setData("BAAAR".getBytes());
        OpenFlowMessage ofFromShim = new OpenFlowMessage();
        ofFromShim.setOfMessage(bresp.build());
        return ofFromShim;
    }

    private OpenFlowMessage getEchoReq(long xid) {
        OFEchoRequest.Builder bquery = fact.buildEchoRequest();

        bquery.setXid(xid);
        bquery.setData("FOOO".getBytes());

        OpenFlowMessage ofMessage = new OpenFlowMessage();
        ofMessage.setOfMessage(bquery.build());
        return ofMessage;
    }
}
