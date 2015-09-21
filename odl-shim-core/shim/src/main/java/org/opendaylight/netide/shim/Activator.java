package org.opendaylight.netide.shim;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("NetIDE SHIM ACTIVATOR: Bundle start");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		LOG.info("NetIDE SHIM ACTIVATOR: Bundle stop");
	}

}