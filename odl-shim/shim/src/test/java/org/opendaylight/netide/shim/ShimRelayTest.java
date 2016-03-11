/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import com.google.common.util.concurrent.Futures;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializationFactory;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializationFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.HelloElementType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.BarrierInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.BarrierOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.BarrierOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoReplyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ExperimenterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetAsyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetAsyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetAsyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetConfigOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetQueueConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetQueueConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetQueueConfigOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GroupModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MeterModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketOutInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PortModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.SetAsyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.SetConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.TableModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.TableModInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.hello.Elements;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.hello.ElementsBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ShimRelayTest {

    @Mock
    ZeroMQBaseConnector coreConnector;

    @Mock
    SerializationFactory factory;

    @Mock
    DeserializationFactory deserializationFactory;

    @Mock
    ConnectionAdapter connectionAdapter;

    @Mock
    ShimRelay shimRelay;

    short ofVersion = EncodeConstants.OF13_VERSION_ID;

    DataObject msg;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(ShimRelayTest.class);
        HelloInputBuilder builder = new HelloInputBuilder();
        builder.setXid(1L);
        builder.setVersion(ofVersion);
        List<Elements> listElements = new ArrayList<>();
        ElementsBuilder elementsBuilder = new ElementsBuilder();
        elementsBuilder.setType(HelloElementType.VERSIONBITMAP);
        List<Boolean> bitmap = new ArrayList<>();
        bitmap.add(true);
        elementsBuilder.setVersionBitmap(bitmap);
        listElements.add(elementsBuilder.build());
        builder.setElements(listElements);
        msg = builder.build();
        Mockito.when(coreConnector.SendData(Matchers.any(byte[].class))).thenReturn(true);
        Mockito.when(deserializationFactory.deserialize(Matchers.any(ByteBuf.class), Mockito.eq(ofVersion)))
                .thenReturn(msg);
        Mockito.when(shimRelay.createDeserializationFactory()).thenReturn(deserializationFactory);
        Mockito.when(shimRelay.createSerializationFactory()).thenReturn(factory);

    }

    @Test
    public void testSendOpenFlowMessageToCore() {
        Mockito.doCallRealMethod().when(shimRelay).sendOpenFlowMessageToCore(coreConnector, msg, ofVersion, 1L, 1, 1);
        shimRelay.sendOpenFlowMessageToCore(coreConnector, msg, ofVersion, 1L, 1, 1);
        Mockito.verify(factory).messageToBuffer(Mockito.eq(ofVersion), Matchers.any(ByteBuf.class), Mockito.eq(msg));
        Mockito.verify(coreConnector).SendData(Matchers.any(byte[].class));
    }

    @Test
    public void testSendToSwitch() {
        ByteBuf input = UnpooledByteBufAllocator.DEFAULT.buffer();
        Mockito.doCallRealMethod().when(shimRelay).sendToSwitch(connectionAdapter, input, ofVersion, coreConnector, 1L,
                1);
        shimRelay.sendToSwitch(connectionAdapter, input, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(shimRelay).sendDataObjectToSwitch(connectionAdapter, msg, ofVersion, coreConnector, 1L, 1);
    }

    @Test
    public void testSendBarrierInputToSwitch() {
        Future<RpcResult<BarrierOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(new BarrierOutputBuilder().build()).build());
        Mockito.when(connectionAdapter.barrier(Matchers.any(BarrierInput.class))).thenReturn(reply);

        BarrierInput message = Mockito.mock(BarrierInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = BarrierInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);

        Mockito.doCallRealMethod().when(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).barrier(Matchers.any(BarrierInput.class));
        Mockito.verify(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(Mockito.eq(coreConnector),
                Matchers.any(BarrierOutput.class), Mockito.eq(ofVersion), Mockito.eq(1L), Mockito.eq(1L),
                Mockito.eq(1));
    }

    @Test
    public void testSendEchoInputToSwitch() {
        Future<RpcResult<EchoOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(new EchoOutputBuilder().build()).build());
        Mockito.when(connectionAdapter.echo(Matchers.any(EchoInput.class))).thenReturn(reply);

        EchoInput message = Mockito.mock(EchoInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = EchoInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);

        Mockito.doCallRealMethod().when(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).echo(Matchers.any(EchoInput.class));
        Mockito.verify(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(Mockito.eq(coreConnector), Matchers.any(EchoOutput.class),
                Mockito.eq(ofVersion), Mockito.eq(1L), Mockito.eq(1L), Mockito.eq(1));
    }

    @Test
    public void testSendEchoOutputToSwitch() {
        EchoOutput message = Mockito.mock(EchoOutput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = EchoOutput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).echoReply(Matchers.any(EchoReplyInput.class));
    }

    @Test
    public void testSendExperimenterInputToSwitch() {
        ExperimenterInput message = Mockito.mock(ExperimenterInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = ExperimenterInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).experimenter(Matchers.any(ExperimenterInput.class));
    }

    @Test
    public void testSendFlowModInputToSwitch() {
        FlowModInput message = Mockito.mock(FlowModInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = FlowModInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).flowMod(Matchers.any(FlowModInput.class));
    }

    @Test
    public void testSendGetAsyncInputToSwitch() {
        GetAsyncOutput messageReply = new GetAsyncOutputBuilder().build();

        Future<RpcResult<GetAsyncOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());
        Mockito.when(connectionAdapter.getAsync(Matchers.any(GetAsyncInput.class))).thenReturn(reply);

        GetAsyncInput message = Mockito.mock(GetAsyncInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = GetAsyncInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);

        Mockito.doCallRealMethod().when(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).getAsync(Matchers.any(GetAsyncInput.class));
        Mockito.verify(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(coreConnector, messageReply, ofVersion, 1L, 1L, 1);
    }

    @Test
    public void testSendGetConfigInputToSwitch() {
        GetConfigOutput messageReply = new GetConfigOutputBuilder().build();

        Future<RpcResult<GetConfigOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());
        Mockito.when(connectionAdapter.getConfig(Matchers.any(GetConfigInput.class))).thenReturn(reply);

        GetConfigInput message = Mockito.mock(GetConfigInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = GetConfigInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);

        Mockito.doCallRealMethod().when(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).getConfig(Matchers.any(GetConfigInput.class));
        Mockito.verify(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(coreConnector, messageReply, ofVersion, 1L, 1L, 1);
    }

    @Test
    public void testSendGetFeaturesInputToSwitch() {
        GetFeaturesOutput messageReply = new GetFeaturesOutputBuilder().build();

        Future<RpcResult<GetFeaturesOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());
        Mockito.when(connectionAdapter.getFeatures(Matchers.any(GetFeaturesInput.class))).thenReturn(reply);

        GetFeaturesInput message = Mockito.mock(GetFeaturesInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = GetFeaturesInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);

        Mockito.doCallRealMethod().when(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).getFeatures(Matchers.any(GetFeaturesInput.class));
        Mockito.verify(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(coreConnector, messageReply, ofVersion, 1L, 1L, 1);
    }

    @Test
    public void testSendGetQueueConfigInputToSwitch() {
        GetQueueConfigOutput messageReply = new GetQueueConfigOutputBuilder().build();

        Future<RpcResult<GetQueueConfigOutput>> reply = Futures
                .immediateFuture(RpcResultBuilder.success(messageReply).build());
        Mockito.when(connectionAdapter.getQueueConfig(Matchers.any(GetQueueConfigInput.class))).thenReturn(reply);

        GetQueueConfigInput message = Mockito.mock(GetQueueConfigInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = GetQueueConfigInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);

        Mockito.doCallRealMethod().when(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).getQueueConfig(Matchers.any(GetQueueConfigInput.class));

        Mockito.verify(shimRelay).sendResponseToCore(reply, coreConnector, ofVersion, 1L, 1, 1);

        Mockito.verify(shimRelay).sendOpenFlowMessageToCore(coreConnector, messageReply, ofVersion, 1L, 1L, 1);
    }

    @Test
    public void testSendGroupModInputToSwitch() {
        GroupModInput message = Mockito.mock(GroupModInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = GroupModInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).groupMod(Matchers.any(GroupModInput.class));
    }

    @Test
    public void testSendHelloInputToSwitch() {
        HelloInput message = Mockito.mock(HelloInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = HelloInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).hello(Matchers.any(HelloInput.class));
    }

    @Test
    public void testSendMeterModInputToSwitch() {
        MeterModInput message = Mockito.mock(MeterModInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = MeterModInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).meterMod(Matchers.any(MeterModInput.class));
    }

    @Test
    public void testSendPacketOutInputToSwitch() {
        PacketOutInput message = Mockito.mock(PacketOutInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = PacketOutInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).packetOut(Matchers.any(PacketOutInput.class));
    }

    @Test
    public void testSendPortModInputToSwitch() {
        PortModInput message = Mockito.mock(PortModInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = PortModInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).portMod(Matchers.any(PortModInput.class));
    }

    @Test
    public void testSendSetAsyncInputToSwitch() {
        SetAsyncInput message = Mockito.mock(SetAsyncInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = SetAsyncInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).setAsync(Matchers.any(SetAsyncInput.class));
    }

    @Test
    public void testSendSetConfigInputToSwitch() {
        SetConfigInput message = Mockito.mock(SetConfigInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = SetConfigInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).setConfig(Matchers.any(SetConfigInput.class));
    }

    @Test
    public void testSendTableModInputToSwitch() {
        TableModInput message = Mockito.mock(TableModInput.class);
        Mockito.doReturn(1L).when(message).getXid();
        String className = TableModInput.class.getName();
        Mockito.doReturn(className).when(shimRelay).getImplementedInterface(message);
        Mockito.doCallRealMethod().when(shimRelay).sendDataObjectToSwitch(connectionAdapter, message, ofVersion,
                coreConnector, 1L, 1);
        shimRelay.sendDataObjectToSwitch(connectionAdapter, message, ofVersion, coreConnector, 1L, 1);
        Mockito.verify(connectionAdapter).tableMod(Matchers.any(TableModInput.class));
    }

    @Test
    public void testGetImplementedInterface() {
        TableModInputBuilder builder = new TableModInputBuilder();
        TableModInput message = builder.build();
        Mockito.doCallRealMethod().when(shimRelay).getImplementedInterface(message);
        Assert.assertEquals(TableModInput.class.getName(), shimRelay.getImplementedInterface(message));
    }

    @Test
    public void testCreateSerializationFactory() {
        Mockito.doCallRealMethod().when(shimRelay).createSerializationFactory();
        Assert.assertEquals(new SerializationFactory().getClass(), shimRelay.createSerializationFactory().getClass());
    }

    @Test
    public void testCreateDeserializationFactory() {
        Mockito.doCallRealMethod().when(shimRelay).createDeserializationFactory();
        Assert.assertEquals(new DeserializationFactory().getClass(),
                shimRelay.createDeserializationFactory().getClass());
    }

}
