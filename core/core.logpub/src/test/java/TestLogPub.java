import eu.netide.core.logpub.LogPub;
import eu.netide.lib.netip.*;
import org.testng.annotations.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMsg;

/**
 * Created by KévinPhemius on 18.08.2015.
 */
public class TestLogPub {

    @Test(timeOut = 10000)
    public void TestStartAndShutdown() {
        LogPub l = new LogPub();
        l.Start();
        ZMQ.Context context = ZMQ.context(1);
        try {
            Thread.sleep(2000);
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
            Thread.sleep(500);
            System.out.println("Message from shim test");
            l.OnShimMessage(m,"s1");
            Thread.sleep(500);
    		System.out.println("SUB test");
            ZMsg zmq_message = new ZMsg();
    		zmq_message.add("1_");
    		zmq_message.add("Profiler");
    		zmq_message.add(m.toByteRepresentation());
    		ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
    		sendSocket.connect("tcp://127.0.0.1:5558");
    		zmq_message.send(sendSocket);
    		Thread.sleep(500);
    		sendSocket.close();
    		Thread.sleep(500);
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
