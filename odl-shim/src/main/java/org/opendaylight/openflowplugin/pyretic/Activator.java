/**
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *     Telefonica I+D
 */
/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowplugin.pyretic;

import com.telefonica.pyretic.backendchannel.BackendChannel;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
// import org.opendaylight.openflowplugin.api.openflow.md.core.session.SessionContext;
//import org.opendaylight.openflowplugin.api.openflow.md.core.session.SwitchSessionKeyOF;
import org.opendaylight.openflowplugin.pyretic.multi.ODLManagerMultiImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


////
//import org.opendaylight.openflowplugin.api.openflow.md.core.session.SessionListener;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;


/**
 * ODL activator
 * 
 * Activator is derived from AbstractBindingAwareConsumer, which takes care
 * of looking up MD-SAL in Service Registry and registering consumer
 * when MD-SAL is present.
 */
public class Activator extends AbstractBindingAwareConsumer implements AutoCloseable {
    
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private ODLManager multiManager;


    @Override
    protected void startImpl(BundleContext context) {
        LOG.info("startImpl() passing");
    }
    
    /**
     * Invoked when consumer is registered to the MD-SAL.
     * 
     */
    @Override
    public void onSessionInitialized(ConsumerContext session) {
        LOG.info("inSessionInitialized() passing");
        /**
         * We create instance of our LearningSwitchManager
         * and set all required dependencies,
         * 
         * which are 
         *   Data Broker (data storage service) - for configuring flows and reading stored switch state
         *   PacketProcessingService - for sending out packets
         *   NotificationService - for receiving notifications such as packet in.
         * 
         */

        this.multiManager = new ODLManagerMultiImpl();

        BackendChannel channel = null;
        try {
            channel = new BackendChannel("localhost",41414);
            channel.setMultiManager(this.multiManager);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.multiManager.setBackendChannel(channel);
        this.multiManager.setDataBroker(session.getSALService(DataBroker.class));
        this.multiManager.setPacketProcessingService(session.getRpcService(PacketProcessingService.class));
        this.multiManager.setNotificationService(session.getSALService(NotificationService.class));
        this.multiManager.start();

    }

    @Override
    public void close() {
        LOG.info("close() passing");
        if (multiManager != null) {
            multiManager.stop();
        }
    }
    
    @Override
    protected void stopImpl(BundleContext context) {
        close();
        super.stopImpl(context);
    }
}