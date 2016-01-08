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
    boolean init = false;

    String exchangeName = "topology-update";
    String baseTopicName = "topology";
    String nodeTopicName = "node";
    String nodeConnectorTopicName = "node_connector";
    String linkTopicName = "link";

    Connection connection;

    public NotificationProducer() {
        init = false;
    }

    public void init(String rabbitHost, int rabbitPort, String rabbitUser, String rabbitPassword,
            String rabbitVirtualHost, String exchangeName, String baseTopicName, String nodeTopicName,
            String nodeConnectorTopicName, String linkTopicName) {

        this.exchangeName = exchangeName;
        this.baseTopicName = baseTopicName;
        this.nodeTopicName = nodeTopicName;
        this.nodeConnectorTopicName = nodeConnectorTopicName;
        this.linkTopicName = linkTopicName;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(rabbitUser);
        factory.setPassword(rabbitPassword);
        factory.setVirtualHost(rabbitVirtualHost);
        factory.setHost(rabbitHost);
        factory.setPort(rabbitPort);
        factory.setAutomaticRecoveryEnabled(true);

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true);
            init = true;
        } catch (IOException e) {
            LOG.error(e.getMessage());
        } catch (TimeoutException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        channel.close();
        connection.close();
    }

    @Override
    public void onNodeConnectorRemoved(NodeConnectorRemoved arg0) {
        if (init) {
            String message = "Node connector removed. Node connector reference: "
                    + arg0.getNodeConnectorRef().getValue();
            publishMessage(message, getTopicName("NodeConnector"));
        }

    }

    @Override
    public void onNodeConnectorUpdated(NodeConnectorUpdated arg0) {
        if (init) {
            String message = "Node connector updated. Node connector reference: "
                    + arg0.getNodeConnectorRef().getValue();
            publishMessage(message, getTopicName("NodeConnector"));
        }

    }

    @Override
    public void onNodeRemoved(NodeRemoved arg0) {
        if (init) {
            String message = "Node removed. Node reference: " + arg0.getNodeRef().getValue();
            publishMessage(message, getTopicName("Node"));
        }
    }

    @Override
    public void onNodeUpdated(NodeUpdated arg0) {
        if (init) {
            String message = "Node updated. Node reference: " + arg0.getNodeRef().getValue();
            publishMessage(message, getTopicName("Node"));
        }
    }

    @Override
    public void onLinkDiscovered(LinkDiscovered notification) {
        if (init) {
            String message = "Link discovered. Source node connector: " + notification.getSource().getValue()
                    + " Destination node connector: " + notification.getDestination().getValue();
            publishMessage(message, getTopicName("Link"));
        }
    }

    @Override
    public void onLinkOverutilized(LinkOverutilized notification) {
        if (init) {
            String message = "Link overutilized. Source node connector: " + notification.getSource().getValue()
                    + " Destination node connector: " + notification.getDestination().getValue();
            publishMessage(message, getTopicName("Link"));
        }
    }

    @Override
    public void onLinkRemoved(LinkRemoved notification) {
        if (init) {
            String message = "Link removed. Source node connector: " + notification.getSource().getValue()
                    + " Destination node connector: " + notification.getDestination().getValue();
            publishMessage(message, getTopicName("Link"));
        }
    }

    @Override
    public void onLinkUtilizationNormal(LinkUtilizationNormal notification) {
        if (init) {
            String message = "Link utilization normal. Source node connector: " + notification.getSource().getValue()
                    + " Destination node connector: " + notification.getDestination().getValue();
            publishMessage(message, getTopicName("Link"));
        }
    }

    private void publishMessage(String message, String topic) {
        try {
            channel.basicPublish(exchangeName, topic, null, message.getBytes());
        } catch (IOException e) {
            LOG.error(e.getMessage());
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
