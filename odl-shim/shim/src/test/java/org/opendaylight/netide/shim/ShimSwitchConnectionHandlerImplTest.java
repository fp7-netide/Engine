/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.is;

import com.google.common.util.concurrent.Futures;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.netide.netiplib.HelloMessage;
import org.opendaylight.netide.netiplib.Protocol;
import org.opendaylight.netide.netiplib.ProtocolVersions;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowplugin.openflow.md.core.sal.SwitchFeaturesUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FeatureCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SwitchFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.hello.Elements;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ShimSwitchConnectionHandlerImplTest {

    ShimSwitchConnectionHandlerImpl connectionHandler;

    @Mock
    NodeUpdated nodeUpdated;

    @Mock
    ZeroMQBaseConnector coreConnector;

    @Mock
    InetAddress address;

    @Mock
    ShimRelay shimRelay;

    @Mock
    ConnectionAdaptersRegistry registry;

    @Mock
    ConnectionAdapter connectionAdapter;

    @Mock
    ByteBuf msg;

    @Mock
    NotificationPublishService notificationProviderService;

    @Mock
    GetFeaturesOutput features;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        connectionHandler = Mockito
                .spy(new ShimSwitchConnectionHandlerImpl(coreConnector, notificationProviderService));
        Mockito.stub(connectionHandler.createShimRelay()).toReturn(shimRelay);
        Mockito.stub(connectionHandler.createConnectionAdaptersRegistry()).toReturn(registry);

        connectionHandler.init();
    }

    @Test
    public void testAccept() {
        Assert.assertEquals(true, connectionHandler.accept(address));
    }

    @Test
    public void testOnSwitchConnected() {
        Mockito.doNothing().when(connectionHandler).handshake(connectionAdapter);
        connectionHandler.onSwitchConnected(connectionAdapter);
        Mockito.verify(registry).registerConnectionAdapter(connectionAdapter, null);
        Mockito.verify(connectionAdapter).setMessageListener(Matchers.any(ShimMessageListener.class));
        Mockito.verify(connectionAdapter).setSystemListener(Matchers.any(ShimMessageListener.class));
        Mockito.verify(connectionAdapter).setConnectionReadyListener(Matchers.any(ShimMessageListener.class));
        Mockito.verify(connectionHandler).handshake(connectionAdapter);
    }

    @Test
    public void testHandshake() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        connectionHandler.handshake(connectionAdapter);
        HelloInputBuilder builder = new HelloInputBuilder();
        builder.setVersion((short) EncodeConstants.OF13_VERSION_ID);
        builder.setXid(ShimSwitchConnectionHandlerImpl.DEFAULT_XID);
        List<Elements> elements = new ArrayList<Elements>();
        builder.setElements(elements);
        Mockito.verify(connectionAdapter).hello(builder.build());
    }

    @Test
    public void testOnSwitchHelloMessage1() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        Mockito.doNothing().when(connectionHandler).sendGetFeaturesToSwitch(Matchers.anyShort(), Matchers.anyLong(),
                Matchers.any(ConnectionAdapter.class));

        connectionHandler.onSwitchHelloMessage(0L, (short) EncodeConstants.OF13_VERSION_ID, connectionAdapter);
        Assert.assertNull(connectionHandler.getSupportedProtocol());
    }

    @Test
    public void testOnSwitchHelloMessage2() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        Mockito.doNothing().when(connectionHandler).sendGetFeaturesToSwitch(Matchers.anyShort(), Matchers.anyLong(),
                Matchers.any(ConnectionAdapter.class));
        connectionHandler.onSwitchHelloMessage(1L, (short) EncodeConstants.OF13_VERSION_ID, connectionAdapter);
        Assert.assertEquals(EncodeConstants.OF13_VERSION_ID,
                connectionHandler.getSupportedProtocol().getValue1().getValue());
    }

    @Test
    public void testOnSwitchHelloMessage3() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF13_VERSION_ID);
        Mockito.doNothing().when(connectionHandler).sendGetFeaturesToSwitch(Matchers.anyShort(), Matchers.anyLong(),
                Matchers.any(ConnectionAdapter.class));
        connectionHandler.onSwitchHelloMessage(1L, (short) EncodeConstants.OF10_VERSION_ID, connectionAdapter);
        Assert.assertEquals(EncodeConstants.OF10_VERSION_ID,
                connectionHandler.getSupportedProtocol().getValue1().getValue());
    }

    @Test
    public void testOnSwitchHelloMessage4() {
        Mockito.stub(connectionHandler.getMaxOFSupportedProtocol()).toReturn(EncodeConstants.OF10_VERSION_ID);
        Mockito.doNothing().when(connectionHandler).sendGetFeaturesToSwitch(Matchers.anyShort(), Matchers.anyLong(),
                Matchers.any(ConnectionAdapter.class));
        connectionHandler.onSwitchHelloMessage(1L, (short) EncodeConstants.OF13_VERSION_ID, connectionAdapter);
        Assert.assertEquals(EncodeConstants.OF10_VERSION_ID,
                connectionHandler.getSupportedProtocol().getValue1().getValue());
    }

    @Test
    public void testOnOpenFlowCoreMessage() {
        Mockito.doReturn(connectionAdapter).when(registry).getConnectionAdapter(1L);
        Mockito.doReturn((short) EncodeConstants.OF13_VERSION_ID).when(msg).readUnsignedByte();
        connectionHandler.onOpenFlowCoreMessage(1L, msg, 0);
        Mockito.verify(shimRelay).sendToSwitch(connectionAdapter, msg, EncodeConstants.OF13_VERSION_ID, coreConnector,
                1L, 0);
    }

    @Test
    public void testOnHelloCoreMessage() {
        Mockito.doNothing().when(connectionHandler).sendGetFeaturesOuputToCore((short) EncodeConstants.OF13_VERSION_ID,
                0, connectionAdapter);

        Pair<Protocol, ProtocolVersions> supportedProtocol = new Pair<Protocol, ProtocolVersions>(Protocol.OPENFLOW,
                ProtocolVersions.parse(Protocol.OPENFLOW, EncodeConstants.OF13_VERSION_ID));
        Mockito.stub(connectionHandler.getSupportedProtocol()).toReturn(supportedProtocol);
        Set<ConnectionAdapter> connections = new HashSet<ConnectionAdapter>();
        connections.add(connectionAdapter);
        Mockito.stub(registry.getConnectionAdapters()).toReturn(connections);
        List<Pair<Protocol, ProtocolVersions>> requestedProtocols = new ArrayList<>();
        requestedProtocols.add(supportedProtocol);
        HelloMessage msg = new HelloMessage();
        msg.getSupportedProtocols().add(supportedProtocol);
        msg.getHeader().setPayloadLength((short) 2);
        msg.getHeader().setModuleId(0);
        connectionHandler.onHelloCoreMessage(requestedProtocols, 0);
        Mockito.verify(coreConnector).SendData(msg.toByteRepresentation());
        Mockito.verify(connectionHandler).sendGetFeaturesOuputToCore((short) EncodeConstants.OF13_VERSION_ID, 0,
                connectionAdapter);
    }

    @Test
    public void testCollectGetFeaturesOutput() {
        Mockito.doReturn(nodeUpdated).when(connectionHandler).nodeAdded(connectionAdapter);
        GetFeaturesOutput messageReply = new GetFeaturesOutputBuilder()
                .setVersion((short) EncodeConstants.OF13_VERSION_ID).build();
        Future<RpcResult<GetFeaturesOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());

        connectionHandler.collectGetFeaturesOuput(reply, connectionAdapter);
        Mockito.verify(registry).registerConnectionAdapter(connectionAdapter, messageReply);
        Mockito.verify(notificationProviderService).offerNotification(Matchers.any(NodeUpdated.class));
    }

    @Test
    public void testSendGetFeaturesToSwitch() {
        GetFeaturesOutput messageReply = new GetFeaturesOutputBuilder()
                .setVersion((short) EncodeConstants.OF13_VERSION_ID).build();

        Future<RpcResult<GetFeaturesOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());
        Mockito.stub(connectionAdapter.getFeatures(Matchers.any(GetFeaturesInput.class))).toReturn(reply);
        Mockito.doNothing().when(connectionHandler).collectGetFeaturesOuput(reply, connectionAdapter);

        connectionHandler.sendGetFeaturesToSwitch((short) EncodeConstants.OF13_VERSION_ID,
                ShimSwitchConnectionHandlerImpl.DEFAULT_XID, connectionAdapter);
        Mockito.verify(connectionHandler).collectGetFeaturesOuput(reply, connectionAdapter);

    }

    @Test
    public void testSendGetFeaturesOuputToCore() {
        GetFeaturesOutput messageReply = new GetFeaturesOutputBuilder().setXid(1L)
                .setVersion((short) EncodeConstants.OF13_VERSION_ID).setDatapathId(new BigInteger("1")).build();
        Future<RpcResult<GetFeaturesOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());
        Mockito.stub(connectionHandler.getFeaturesFromRegistry(connectionAdapter)).toReturn(messageReply);
        connectionHandler.sendGetFeaturesOuputToCore((short) EncodeConstants.OF13_VERSION_ID, 0, connectionAdapter);

        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(coreConnector, messageReply,
                EncodeConstants.OF13_VERSION_ID, 1L, 1L, 0);
    }

    @Test
    public void testGetSupportedOFProtocols() {
        List<Byte> results = new ArrayList<>();
        results.add(ProtocolVersions.OPENFLOW_1_0.getValue());
        results.add(ProtocolVersions.OPENFLOW_1_3.getValue());
        Assert.assertThat(results, is(connectionHandler.getSupportedOFProtocols()));
    }

    @Test
    public void testNodeAdded() {
        GetFeaturesOutputBuilder featuresBuilder = new GetFeaturesOutputBuilder();
        GetFeaturesOutput features = featuresBuilder.build();
        Mockito.stub(registry.getDatapathID(connectionAdapter)).toReturn(new BigInteger("1"));
        Mockito.stub(registry.getFeaturesOutput(connectionAdapter)).toReturn(features);
        Mockito.stub(connectionAdapter.getRemoteAddress()).toReturn(new InetSocketAddress(1));

        NodeUpdatedBuilder builder = new NodeUpdatedBuilder();
        String current = String.valueOf(new BigInteger("1"));
        builder.setId(new NodeId("openflow:" + current));
        InstanceIdentifier<Node> identifier = connectionHandler.identifierFromDatapathId(new BigInteger("1"));
        builder.setNodeRef(new NodeRef(identifier));

        FlowCapableNodeUpdatedBuilder builder2 = new FlowCapableNodeUpdatedBuilder();
        try {
            builder2.setIpAddress(resolveIpAddress(new InetSocketAddress(1).getAddress()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwitchFeatures swFeatures = new SwitchFeatures() {

            @Override
            public <E extends Augmentation<SwitchFeatures>> E getAugmentation(Class<E> arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Short getMaxTables() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Long getMaxBuffers() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<Class<? extends FeatureCapability>> getCapabilities() {
                // TODO Auto-generated method stub
                return null;
            }
        };

        SwitchFeaturesUtil swFeaturesUtil = SwitchFeaturesUtil.getInstance();

        builder2.setSwitchFeatures(swFeaturesUtil.buildSwitchFeatures(features));
        builder.addAugmentation(FlowCapableNodeUpdated.class, builder2.build());

        Assert.assertEquals(builder.build(), connectionHandler.nodeAdded(connectionAdapter));
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

}
