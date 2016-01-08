/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.netide.util.topology.update.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
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

    Channel channel;
    String exchangeName = "topology-update";
    String baseTopicName = "topology";
    String nodeTopicName = "node";
    String nodeConnectorTopicName = "node_connector";
    String linkTopicName = "link";
    Connection connection;

    public NotificationProducer() {
        LOG.info("NOTIFICATION PROVIDER:Constructor");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("opendaylight");
        factory.setPassword("opendaylight");
        factory.setVirtualHost("/opendaylight");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setAutomaticRecoveryEnabled(true);

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        channel.close();
        connection.close();
    }

    @Override
    public void onNodeConnectorRemoved(NodeConnectorRemoved arg0) {
        LOG.info("NotificationProducer: onNodeConnectorRemoved {}", arg0.getNodeConnectorRef().getValue());
        String message = "Node connector removed. Node connector reference: " + arg0.getNodeConnectorRef().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("NodeConnector"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNodeConnectorUpdated(NodeConnectorUpdated arg0) {
        LOG.info("NotificationProducer: onNodeConnectorUpdated, {}", arg0.getNodeConnectorRef().getValue());
        String message = "Node connector updated. Node connector reference: " + arg0.getNodeConnectorRef().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("NodeConnector"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNodeRemoved(NodeRemoved arg0) {
        LOG.info("NotificationProducer: onNodeRemoved, {}", arg0.getNodeRef().getValue());
        String message = "Node removed. Node reference: " + arg0.getNodeRef().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("Node"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNodeUpdated(NodeUpdated arg0) {
        LOG.info("NotificationProducer: onNodeUpdated, {}", arg0.getNodeRef().getValue());
        String message = "Node updated. Node reference: " + arg0.getNodeRef().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("Node"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLinkDiscovered(LinkDiscovered notification) {
        LOG.info("NotificationProducer: onLinkDiscovered, Source: {}, Destination: {}, ", notification.getSource(),
                notification.getDestination());

        String message = "Link discovered. Source node connector: " + notification.getSource().getValue()
                + " Destination node connector: " + notification.getDestination().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("Link"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLinkOverutilized(LinkOverutilized notification) {
        LOG.info("NotificationProducer: onLinkOverutilized, Source: {}, Destination: {}, ", notification.getSource(),
                notification.getDestination());

        String message = "Link overutilized. Source node connector: " + notification.getSource().getValue()
                + " Destination node connector: " + notification.getDestination().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("Link"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLinkRemoved(LinkRemoved notification) {
        LOG.info("NotificationProducer: onLinkRemoved, Source: {}, Destination: {}, ", notification.getSource(),
                notification.getDestination());

        String message = "Link removed. Source node connector: " + notification.getSource().getValue()
                + " Destination node connector: " + notification.getDestination().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("Link"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLinkUtilizationNormal(LinkUtilizationNormal notification) {
        LOG.info("NotificationProducer: onLinkUtilizationNormal, Source: {}, Destination: {}, ",
                notification.getSource(), notification.getDestination());

        String message = "Link utilization normal. Source node connector: " + notification.getSource().getValue()
                + " Destination node connector: " + notification.getDestination().getValue();
        try {
            channel.basicPublish(exchangeName, getTopicName("Link"), null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getTopicName(String key) {
        String topicName = baseTopicName + ".";
        switch (key) {
        case "NodeConnector":
            topicName += nodeConnectorTopicName;
            break;
        case "Node":
            topicName += nodeTopicName;
            break;
        case "Link":
            topicName += linkTopicName;
            break;
        default:
            return null;
        }
        return topicName;
    }
}
