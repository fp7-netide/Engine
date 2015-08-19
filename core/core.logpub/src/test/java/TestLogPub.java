import eu.netide.core.logpub.LogPub;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.MessageHeader;
import eu.netide.lib.netip.MessageType;
import eu.netide.lib.netip.NetIDEProtocolVersion;
import org.testng.annotations.Test;
import org.zeromq.ZMQ;

/**
 * Created by KévinPhemius on 18.08.2015.
 */
public class TestLogPub {

    @Test
    public void TestStartAndShutdown() {
        ZMQ.Context context = ZMQ.context(1);
        LogPub l = new LogPub();
        l.Start();
        try {
            Thread.sleep(100);
            System.out.println("Message from backend test");
            MessageHeader mh = new MessageHeader();
            mh.setDatapathId(42);
            mh.setMessageType(MessageType.HELLO);
            mh.setModuleId(23);
            mh.setNetIDEProtocolVersion(NetIDEProtocolVersion.VERSION_1_0);
            mh.setPayloadLength((short) 5);
            mh.setTransactionId(0);
            byte[] p = new byte[5];
            Message m = new Message(mh,p);
            l.OnBackendMessage(m, "b1");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Control test");
            l.Stop();
        }
    }
}
