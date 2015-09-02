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

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Bundle start");
		connectionProvider  = new SwitchConnectionProviderImpl();
		ShimSwitchConnectionHandlerImpl handler = new ShimSwitchConnectionHandlerImpl();
		connectionProvider.setSwitchConnectionHandler(handler);
		
		ConnectionConfiguration conf = new ShimConnectionConfiguration();
		
		connectionProvider.setConfiguration(conf);
		connectionProvider.startup();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Bundle stop");
		connectionProvider.shutdown();
	}

}