package org.opendaylight.netide.shim;


import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration;
import org.opendaylight.openflowjava.protocol.impl.core.SwitchConnectionProviderImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator{

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private SwitchConnectionProviderImpl connectionProvider;
    private ZeroMQBaseConnector coreConnector;

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Bundle start");
		connectionProvider  = new SwitchConnectionProviderImpl();
		coreConnector = new ZeroMQBaseConnector();
		
		ShimSwitchConnectionHandlerImpl handler = new ShimSwitchConnectionHandlerImpl(coreConnector);
		coreConnector.RegisterCoreListener(handler);
		coreConnector.setPort(5555);
		
		connectionProvider.setSwitchConnectionHandler(handler);

		ConnectionConfiguration conf = new ShimConnectionConfiguration();
		
		connectionProvider.setConfiguration(conf);
		connectionProvider.startup();
		coreConnector.Start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Bundle stop");
		coreConnector.Stop();
		connectionProvider.shutdown();
	}

}