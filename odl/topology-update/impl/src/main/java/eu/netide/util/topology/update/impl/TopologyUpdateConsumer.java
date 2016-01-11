/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.netide.util.topology.update.impl;

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
        producer = new NotificationProducer();
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
        notificationService.registerNotificationListener(producer);
    }

    public void configureNotificationProducer(String rabbitHost, int rabbitPort, String rabbitUser,
            String rabbitPassword, String rabbitVirtualHost, String exchangeName, String baseTopicName,
            String nodeTopicName, String nodeConnectorTopicName, String linkTopicName) {

        producer.init(rabbitHost, rabbitPort, rabbitUser, rabbitPassword, rabbitVirtualHost, exchangeName,
                baseTopicName, nodeTopicName, nodeConnectorTopicName, linkTopicName);
    }

}
