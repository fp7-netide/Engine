/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import org.javatuples.Pair;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.netide.netiplib.HelloMessage;
import org.opendaylight.netide.netiplib.Protocol;
import org.opendaylight.netide.netiplib.ProtocolVersions;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.connection.SwitchConnectionHandler;
import org.opendaylight.openflowplugin.openflow.md.core.sal.SwitchFeaturesUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.hello.Elements;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShimSwitchConnectionHandlerImpl implements SwitchConnectionHandler, ICoreListener, IHandshakeListener {
    public static final Long DEFAULT_XID = 0x01L;
    private static final Logger LOG = LoggerFactory.getLogger(ShimSwitchConnectionHandlerImpl.class);

    private static ZeroMQBaseConnector coreConnector;
    private ConnectionAdaptersRegistry connectionRegistry;
    private Pair<Protocol, ProtocolVersions> supportedProtocol;
    List<Pair<Protocol, ProtocolVersions>> supportedProtocols;
    private ShimRelay shimRelay;
    private NotificationPublishService notificationProviderService;
    HashMap<InetSocketAddress, ShimMessageListener> mapListeners;
    HashMap<InetSocketAddress, GetFeaturesOutput> mapFeatures;
    SwitchFeaturesUtil swFeaturesUtil;

    public ShimSwitchConnectionHandlerImpl(ZeroMQBaseConnector connector,
            NotificationPublishService _notificationProviderService) {
        coreConnector = connector;
        supportedProtocol = null;
        supportedProtocols = new ArrayList<>();
        mapListeners = new HashMap<>();
        mapFeatures = new HashMap<>();
        notificationProviderService = _notificationProviderService;
        swFeaturesUtil = SwitchFeaturesUtil.getInstance();
    }

    public void setSwitchFeaturesUtil(SwitchFeaturesUtil featureUtil) {
        swFeaturesUtil = featureUtil;
    }

    public void init() {
        supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_0));
        supportedProtocols.add(new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW, ProtocolVersions.OPENFLOW_1_3));
        connectionRegistry = createConnectionAdaptersRegistry();
        connectionRegistry.init();
        shimRelay = createShimRelay();
    }

    public ShimRelay createShimRelay() {
        return new ShimRelay();
    }

    public ConnectionAdaptersRegistry createConnectionAdaptersRegistry() {
        return new ConnectionAdaptersRegistry();
    }

    @Override
    public boolean accept(InetAddress arg0) {
        return true;
    }

    @Override
    public void onSwitchConnected(ConnectionAdapter connectionAdapter) {
        LOG.info("CREATING NEW LISTENER FOR {}", connectionAdapter.getRemoteAddress());
        ShimMessageListener listener = new ShimMessageListener(coreConnector, connectionAdapter, shimRelay, this,
                notificationProviderService);

        mapListeners.put(connectionAdapter.getRemoteAddress(), listener);
        listener.registerConnectionAdaptersRegistry(connectionRegistry);
        listener.registerHandshakeListener(this);
        connectionRegistry.registerConnectionAdapter(connectionAdapter, null);
        connectionAdapter.setMessageListener(listener);
        connectionAdapter.setSystemListener(listener);
        connectionAdapter.setConnectionReadyListener(listener);
        handshake(connectionAdapter);
    }

    public void handshake(ConnectionAdapter connectionAdapter) {
        HelloInputBuilder builder = new HelloInputBuilder();
        builder.setVersion((short) getMaxOFSupportedProtocol());
        builder.setXid(DEFAULT_XID);
        List<Elements> elements = new ArrayList<Elements>();
        builder.setElements(elements);
        connectionAdapter.hello(builder.build());
    }

    @Override
    public void onSwitchHelloMessage(long xid, Short version, ConnectionAdapter connectionAdapter) {
        byte received = version.byteValue();
        if (xid >= DEFAULT_XID) {
            if (received <= getMaxOFSupportedProtocol()) {
                setSupportedProtocol(received);

            } else {
                setSupportedProtocol(getMaxOFSupportedProtocol());
            }
        }
        sendGetFeaturesToSwitch(version, (xid + 1), connectionAdapter);
    }

    public NodeUpdated nodeAdded(ConnectionAdapter connectionAdapter) {
        NodeUpdatedBuilder builder = new NodeUpdatedBuilder();
        BigInteger datapathId = this.connectionRegistry.getDatapathID(connectionAdapter);
        builder.setId(nodeIdFromDatapathId(datapathId));
        InstanceIdentifier<Node> identifier = identifierFromDatapathId(datapathId);
        builder.setNodeRef(new NodeRef(identifier));

        FlowCapableNodeUpdatedBuilder builder2 = new FlowCapableNodeUpdatedBuilder();
        try {
            builder2.setIpAddress(getIpAddressOf(connectionAdapter));
        } catch (Exception e) {
            LOG.warn("IP address of the node cannot be obtained.");
        }
        GetFeaturesOutput features = this.connectionRegistry.getFeaturesOutput(connectionAdapter);

        builder2.setSwitchFeatures(swFeaturesUtil.buildSwitchFeatures(features));
        builder.addAugmentation(FlowCapableNodeUpdated.class, builder2.build());

        return builder.build();
    }

    private static IpAddress getIpAddressOf(final ConnectionAdapter connectionAdapter) {

        InetSocketAddress remoteAddress = connectionAdapter.getRemoteAddress();
        if (remoteAddress == null) {
            LOG.warn("IP address of the node cannot be obtained. No connection with switch.");
            return null;
        }
        return resolveIpAddress(remoteAddress.getAddress());
    }

    private static IpAddress resolveIpAddress(final InetAddress address) {
        String hostAddress = address.getHostAddress();
        if (address instanceof Inet4Address) {
            return new IpAddress(new Ipv4Address(hostAddress));
        }
        if (address instanceof Inet6Address) {
            return new IpAddress(new Ipv6Address(hostAddress));
        }
        throw new IllegalArgumentException("Unsupported IP address type!");
    }

    public static InstanceIdentifier<Node> identifierFromDatapathId(final BigInteger datapathId) {
        NodeKey nodeKey = nodeKeyFromDatapathId(datapathId);
        InstanceIdentifierBuilder<Node> builder = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey);
        return builder.build();
    }

    public static NodeKey nodeKeyFromDatapathId(final BigInteger datapathId) {
        return new NodeKey(nodeIdFromDatapathId(datapathId));
    }

    public static NodeId nodeIdFromDatapathId(final BigInteger datapathId) {
        // FIXME: Convert to textual representation of datapathID
        String current = String.valueOf(datapathId);
        return new NodeId("openflow:" + current);
    }

    public byte getMaxOFSupportedProtocol() {
        byte max = 0x00;
        for (Pair<Protocol, ProtocolVersions> protocols : this.supportedProtocols) {
            if (protocols.getValue0() == Protocol.OPENFLOW && protocols.getValue1().getValue() > max) {
                max = protocols.getValue1().getValue();
            }
        }
        return max;
    }

    public List<Byte> getSupportedOFProtocols() {
        List<Byte> results = new ArrayList<>();
        for (Pair<Protocol, ProtocolVersions> protocols : this.supportedProtocols) {
            if (protocols.getValue0() == Protocol.OPENFLOW) {
                results.add(protocols.getValue1().getValue());
            }
        }
        return results;
    }

    public int getNumberOfSwitches() {
        return this.connectionRegistry.getConnectionAdapters().size();
    }

    public Pair<Protocol, ProtocolVersions> getSupportedProtocol() {
        return this.supportedProtocol;
    }

    public void setSupportedProtocol(byte version) {
        this.supportedProtocol = new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW,
                ProtocolVersions.parse(Protocol.OPENFLOW, version));
    }

    @Override
    public void onOpenFlowCoreMessage(Long datapathId, ByteBuf msg, int moduleId) {
        ConnectionAdapter conn = connectionRegistry.getConnectionAdapter(datapathId);

        if (conn != null) {
            short ofVersion = msg.readUnsignedByte();
            shimRelay.sendToSwitch(conn, msg, ofVersion, coreConnector, datapathId, moduleId);
        }
    }

    @Override
    public void onHelloCoreMessage(List<Pair<Protocol, ProtocolVersions>> requestedProtocols, int moduleId) {
        for (Pair<Protocol, ProtocolVersions> requested : requestedProtocols) {
            if (getSupportedProtocol() != null) {
                if (requested.getValue0().getValue() == getSupportedProtocol().getValue0().getValue()
                        && requested.getValue1().getValue() == getSupportedProtocol().getValue1().getValue()) {
                    HelloMessage msg = new HelloMessage();
                    msg.getSupportedProtocols().add(getSupportedProtocol());
                    msg.getHeader().setPayloadLength((short) 2);
                    msg.getHeader().setModuleId(moduleId);
                    coreConnector.SendData(msg.toByteRepresentation());
                    for (ConnectionAdapter conn : connectionRegistry.getConnectionAdapters()) {
                        sendGetFeaturesOuputToCore((short) getSupportedProtocol().getValue1().getValue(), moduleId,
                                conn);

                    }
                }
            }
        }
    }

    public void collectGetFeaturesOuput(Future<RpcResult<GetFeaturesOutput>> switchReply,
            final ConnectionAdapter connectionAdapter) {
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(switchReply),
                new FutureCallback<RpcResult<GetFeaturesOutput>>() {
                    @Override
                    public void onSuccess(RpcResult<GetFeaturesOutput> rpcFeatures) {
                        if (rpcFeatures.isSuccessful()) {
                            GetFeaturesOutput featureOutput = rpcFeatures.getResult();
                            // Register Switch connection/DatapathId to registry
                            connectionRegistry.registerConnectionAdapter(connectionAdapter, featureOutput);
                            NodeUpdated nodeUpdated = nodeAdded(connectionAdapter);
                            notificationProviderService.offerNotification(nodeUpdated);

                        } else {
                            // Handshake failed
                            for (RpcError rpcError : rpcFeatures.getErrors()) {
                                LOG.info("handshake - features failure [{}]: i:{} | m:{} | s:{}", rpcError.getInfo(),
                                        rpcError.getMessage(), rpcError.getSeverity(), rpcError.getCause());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOG.info("getting feature failed seriously [addr:{}]: {}", connectionAdapter.getRemoteAddress(),
                                t.getMessage());
                    }
                });
    }

    public void sendGetFeaturesOuputToCore(final Short proposedVersion, final int moduleId,
            final ConnectionAdapter connectionAdapter) {

        GetFeaturesOutput featureOutput = getFeaturesFromRegistry(connectionAdapter);
        shimRelay.sendOpenFlowMessageToCore(ShimSwitchConnectionHandlerImpl.coreConnector, featureOutput,
                proposedVersion, featureOutput.getXid(), featureOutput.getDatapathId().shortValue(), moduleId);
    }

    public void sendGetFeaturesToSwitch(final Short proposedVersion, final Long xid,
            final ConnectionAdapter connectionAdapter) {

        GetFeaturesInputBuilder featuresBuilder = new GetFeaturesInputBuilder();
        featuresBuilder.setVersion(proposedVersion).setXid(xid);

        Future<RpcResult<GetFeaturesOutput>> featuresFuture = connectionAdapter.getFeatures(featuresBuilder.build());
        collectGetFeaturesOuput(featuresFuture, connectionAdapter);
    }

    private static NodeRemoved nodeRemoved(final NodeRef nodeRef) {
        NodeRemovedBuilder builder = new NodeRemovedBuilder();
        builder.setNodeRef(nodeRef);
        return builder.build();
    }

    @Override
    public void onSwitchDisconnected(ConnectionAdapter connectionAdapter) {
        BigInteger datapathId = connectionRegistry.getDatapathID(connectionAdapter);
        InstanceIdentifier<Node> identifier = identifierFromDatapathId(datapathId);
        NodeRef nodeRef = new NodeRef(identifier);
        NodeRemoved nodeRemoved = nodeRemoved(nodeRef);
        notificationProviderService.offerNotification(nodeRemoved);

        connectionRegistry.removeConnectionAdapter(connectionAdapter);
    }

    public GetFeaturesOutput getFeaturesFromRegistry(ConnectionAdapter conn) {
        return this.connectionRegistry.getFeaturesOutput(conn);
    }

}