package org.opendaylight.netide.shim;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;

import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BindingAwareConsumer, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	public void close() throws Exception {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Session Closed");
	}

	public void onSessionInitialized(ConsumerContext arg0) {
		// TODO Auto-generated method stub
		LOG.info("ODL SHIM CORE ACTIVATOR: Session Initialized");
	}

   

}