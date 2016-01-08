/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.topology.update.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyUpdateConsumer implements BindingAwareConsumer, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyUpdateConsumer.class);

    NotificationProducer producer;
    NotificationProviderService notificationService;

    public TopologyUpdateConsumer(NotificationProviderService _notificationService) {
        notificationService = _notificationService;
    }

    @Override
    public void close() throws Exception {
        LOG.info("TopologyUpdateProvider Closed");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.sal.binding.api.BindingAwareConsumer#
     * onSessionInitialized(org.opendaylight.controller.sal.binding.api.
     * BindingAwareBroker.ConsumerContext)
     */
    @Override
    public void onSessionInitialized(ConsumerContext context) {
        LOG.info("TopologyUpdateProvider onSessionInitiated");
        producer = new NotificationProducer();
        notificationService.registerNotificationListener(producer);
    }

}
