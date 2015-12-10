/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.topology.update.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkOverutilized;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkUtilizationNormal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class NotificationProducer
        implements OpendaylightInventoryListener, AutoCloseable, FlowTopologyDiscoveryListener {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationProducer.class);

    public NotificationProducer() {
        LOG.info("NOTIFICATION PROVIDER:Constructor");
        // notificationService.registerNotificationListener(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.
     * OpendaylightInventoryListener#onNodeConnectorRemoved(org.opendaylight.
     * yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved)
     */
    @Override
    public void onNodeConnectorRemoved(NodeConnectorRemoved arg0) {
        LOG.info("NotificationProducer: onNodeConnectorRemoved {}", arg0.getNodeConnectorRef().getValue());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.
     * OpendaylightInventoryListener#onNodeConnectorUpdated(org.opendaylight.
     * yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated)
     */
    @Override
    public void onNodeConnectorUpdated(NodeConnectorUpdated arg0) {
        LOG.info("NotificationProducer: onNodeConnectorUpdated, {}", arg0.getNodeConnectorRef().getValue());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.
     * OpendaylightInventoryListener#onNodeRemoved(org.opendaylight.yang.gen.v1.
     * urn.opendaylight.inventory.rev130819.NodeRemoved)
     */
    @Override
    public void onNodeRemoved(NodeRemoved arg0) {
        LOG.info("NotificationProducer: onNodeRemoved, {}", arg0.getNodeRef().getValue());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.
     * OpendaylightInventoryListener#onNodeUpdated(org.opendaylight.yang.gen.v1.
     * urn.opendaylight.inventory.rev130819.NodeUpdated)
     */
    @Override
    public void onNodeUpdated(NodeUpdated arg0) {
        LOG.info("NotificationProducer: onNodeUpdated, {}", arg0.getNodeRef().getValue());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.
     * rev130819.FlowTopologyDiscoveryListener#onLinkDiscovered(org.opendaylight
     * .yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.
     * LinkDiscovered)
     */
    @Override
    public void onLinkDiscovered(LinkDiscovered notification) {
        LOG.info("NotificationProducer: onLinkDiscovered, Source: {}, Destination: {}, ", notification.getSource(),
                notification.getDestination());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.
     * rev130819.FlowTopologyDiscoveryListener#onLinkOverutilized(org.
     * opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.
     * rev130819.LinkOverutilized)
     */
    @Override
    public void onLinkOverutilized(LinkOverutilized notification) {
        LOG.info("NotificationProducer: onLinkOverutilized, Source: {}, Destination: {}, ", notification.getSource(),
                notification.getDestination());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.
     * rev130819.FlowTopologyDiscoveryListener#onLinkRemoved(org.opendaylight.
     * yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.
     * LinkRemoved)
     */
    @Override
    public void onLinkRemoved(LinkRemoved notification) {
        LOG.info("NotificationProducer: onLinkRemoved, Source: {}, Destination: {}, ", notification.getSource(),
                notification.getDestination());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.
     * rev130819.FlowTopologyDiscoveryListener#onLinkUtilizationNormal(org.
     * opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.
     * rev130819.LinkUtilizationNormal)
     */
    @Override
    public void onLinkUtilizationNormal(LinkUtilizationNormal notification) {
        LOG.info("NotificationProducer: onLinkUtilizationNormal, Source: {}, Destination: {}, ",
                notification.getSource(), notification.getDestination());
    }
}
