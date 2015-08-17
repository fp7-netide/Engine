import eu.netide.core.connectivity.ZeroMQBasedConnector;
import org.testng.annotations.Test;

/**
 * Created by timvi on 11.08.2015.
 */
public class TestShimConnector {

    @Test
    public void TestStartAndShutdown() {
        ZeroMQBasedConnector c = new ZeroMQBasedConnector();
        c.Start();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            c.Stop();
        }
    }
}
