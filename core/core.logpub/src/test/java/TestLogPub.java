import eu.netide.core.logpub.LogPub;
import eu.netide.lib.netip.*;
import org.testng.annotations.Test;
import org.zeromq.ZMQ;

/**
 * Created by K�vinPhemius on 18.08.2015.
 */
public class TestLogPub {

    @Test
    public void TestStartAndShutdown() {
        LogPub l = new LogPub();
        l.Start();
        try {
            Thread.sleep(100);
            // Building message
            MessageHeader mh = new MessageHeader();
            mh.setDatapathId(42);
            mh.setMessageType(MessageType.HELLO);
            mh.setModuleId(23);
            mh.setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_0);
            mh.setPayloadLength((short) 4);
            mh.setTransactionId(8);
            byte[] p = new byte[4];
            Message m = new Message(mh,p);
            //
            System.out.println("Message from backend test");
            l.OnBackendMessage(m, "b1");
            System.out.println("Message from shim test");
            l.OnShimMessage(m,"s1");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Control test");
            ManagementMessage m = new ManagementMessage();
            m.setHeader(new MessageHeader());
            m.getHeader().setMessageType(MessageType.MANAGEMENT);
            m.setPayloadString("Control.STOP");
            l.OnManagementMessage(m);
        }
    }
}
