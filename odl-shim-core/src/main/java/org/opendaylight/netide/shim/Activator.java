package org.opendaylight.netide.shim;


import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration;
import org.opendaylight.openflowjava.protocol.impl.core.SwitchConnectionProviderImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import eu.netide.lib.netip.HelloMessage;

public class Activator implements BundleActivator{

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private SwitchConnectionProviderImpl connectionProvider;

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Bundle start");
		ZMQ.Context zmqContext = ZMQ.context(1);
		ZMQ.Socket socket = zmqContext.socket(ZMQ.DEALER);
		socket.setIdentity("shim".getBytes(ZMQ.CHARSET));
		LOG.info("Connecting to Core...");
		socket.connect("tcp://localhost:5555");
		LOG.info("Connected to Core...");
		HelloMessage hello = new HelloMessage();
		socket.send(hello.toByteRepresentation());
		ZMQ.Poller poller = new ZMQ.Poller(1);
		poller.register(socket, ZMQ.Poller.POLLIN);
		connectionProvider  = new SwitchConnectionProviderImpl();
		ShimSwitchConnectionHandlerImpl handler = new ShimSwitchConnectionHandlerImpl(socket);
		connectionProvider.setSwitchConnectionHandler(handler);

		ConnectionConfiguration conf = new ShimConnectionConfiguration();
		
		connectionProvider.setConfiguration(conf);
		connectionProvider.startup();
		
		while(true) {
			int signalled = poller.poll(10);
			if (signalled == 1) { // A message is available
                // Reading a multi-part message
                ZMsg message = ZMsg.recvMsg(socket);
                String senderId = message.getFirst().toString();
                String payload = message.getLast().toString();
                LOG.info("Received message from '" + senderId + "': " + payload);

                // Sending a message
                socket.send("Pong.");
                System.out.println("Reply sent.");
            }
            // Allow interruption
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                break;
            }
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Bundle stop");
		connectionProvider.shutdown();
	}

}