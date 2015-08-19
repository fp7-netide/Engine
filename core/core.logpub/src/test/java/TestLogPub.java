import eu.netide.core.logpub.LogPub;
import org.testng.annotations.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

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
            // Test data queue
            System.out.println("Data test");
            ZMsg msg = new ZMsg();
            msg.add("shim");
            msg.add("TEST");
            ZMQ.Socket sendSocket = context.socket(ZMQ.PUSH);
            sendSocket.connect("ipc://LogPubData");
            msg.send(sendSocket);
            sendSocket.close();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Control test");
            l.Stop();
        }
    }
}
