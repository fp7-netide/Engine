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
import io.netty.buffer.UnpooledByteBufAllocator;
import java.util.concurrent.Future;
import org.opendaylight.netide.netiplib.Message;
import org.opendaylight.netide.netiplib.MessageType;
import org.opendaylight.netide.netiplib.NetIPUtils;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistry;
import org.opendaylight.openflowjava.protocol.api.extensibility.SerializerRegistry;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializationFactory;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializerRegistryImpl;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializationFactory;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializerRegistryImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.BarrierInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.BarrierOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoReplyInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoRequestMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ExperimenterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetAsyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetAsyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetQueueConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetQueueConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GroupModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MeterModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketOutInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PortModInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.RoleRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.RoleRequestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.SetAsyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.SetConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.TableModInput;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author giuseppex.petralia@intel.com
 *
 */
public class ShimRelay {
    private static final Logger LOG = LoggerFactory.getLogger(ShimRelay.class);

    public SerializationFactory createSerializationFactory() {
        return new SerializationFactory();
    }

    public DeserializationFactory createDeserializationFactory() {
        return new DeserializationFactory();
    }

    public void sendOpenFlowMessageToCore(ZeroMQBaseConnector coreConnector, DataObject msg, short ofVersion, long xId,
            long datapathId, int moduleId) {

        SerializationFactory factory = createSerializationFactory();
        SerializerRegistry registry = new SerializerRegistryImpl();
        registry.init();
        ByteBuf output = UnpooledByteBufAllocator.DEFAULT.buffer();
        factory.setSerializerTable(registry);
        factory.messageToBuffer(ofVersion, output, msg);
        byte[] bytes = new byte[output.readableBytes()];
        output.readBytes(bytes);
        Message message = new Message(NetIPUtils.StubHeaderFromPayload(bytes), bytes);
        message.getHeader().setMessageType(MessageType.OPENFLOW);
        message.getHeader().setDatapathId(datapathId);
        message.getHeader().setModuleId(moduleId);
        message.getHeader().setTransactionId((int) xId);
        coreConnector.SendData(message.toByteRepresentation());
    }

    public void sendToSwitch(ConnectionAdapter connectionAdapter, ByteBuf input, short ofVersion,
            ZeroMQBaseConnector coreConnector, long datapathId, int moduleId) {

        DeserializationFactory factory = createDeserializationFactory();
        DeserializerRegistry registry = new DeserializerRegistryImpl();
        registry.init();
        factory.setRegistry(registry);
        DataObject msg = factory.deserialize(input, ofVersion);
        sendDataObjectToSwitch(connectionAdapter, msg, ofVersion, coreConnector, datapathId, moduleId);
    }

    public void sendDataObjectToSwitch(ConnectionAdapter connectionAdapter, DataObject msg, short ofVersion,
            ZeroMQBaseConnector coreConnector, long datapathId, int moduleId) {

        if (getImplementedInterface(msg).equals(BarrierInput.class.getName())) {
            Future<RpcResult<BarrierOutput>> reply = connectionAdapter.barrier((BarrierInput) msg);
            sendResponseToCore(reply, coreConnector, ofVersion, ((BarrierInput) msg).getXid(), datapathId, moduleId);

        } else if (getImplementedInterface(msg).equals(EchoInput.class.getName())) {

            Future<RpcResult<EchoOutput>> reply = connectionAdapter.echo((EchoInput) msg);
            sendResponseToCore(reply, coreConnector, ofVersion, ((EchoInput) msg).getXid(), datapathId, moduleId);

        } else if (getImplementedInterface(msg).equals(EchoRequestMessage.class.getName())) {
            EchoInputBuilder builder = new EchoInputBuilder();
            EchoRequestMessage echoRequestMessage = (EchoRequestMessage) msg;
            if (echoRequestMessage.getData() != null)
                builder.setData(echoRequestMessage.getData());
            builder.setVersion(echoRequestMessage.getVersion());
            builder.setXid(echoRequestMessage.getXid());
            Future<RpcResult<EchoOutput>> reply = connectionAdapter.echo(builder.build());
            sendResponseToCore(reply, coreConnector, ofVersion, echoRequestMessage.getXid(), datapathId, moduleId);

        } else if (getImplementedInterface(msg).equals(EchoOutput.class.getName())) {

            EchoReplyInputBuilder builder = new EchoReplyInputBuilder();
            builder.setVersion(((EchoOutput) msg).getVersion());
            builder.setXid(((EchoOutput) msg).getXid());
            builder.setData(((EchoOutput) msg).getData());
            connectionAdapter.echoReply(builder.build());

        } else if (getImplementedInterface(msg).equals(ExperimenterInput.class.getName())) {

            connectionAdapter.experimenter((ExperimenterInput) msg);

        } else if (getImplementedInterface(msg).equals(FlowModInput.class.getName())) {

            connectionAdapter.flowMod((FlowModInput) msg);

        } else if (getImplementedInterface(msg).equals(GetAsyncInput.class.getName())) {

            Future<RpcResult<GetAsyncOutput>> reply = connectionAdapter.getAsync((GetAsyncInput) msg);
            sendResponseToCore(reply, coreConnector, ofVersion, ((GetAsyncInput) msg).getXid(), datapathId, moduleId);

        } else if (getImplementedInterface(msg).equals(GetConfigInput.class.getName())) {

            Future<RpcResult<GetConfigOutput>> reply = connectionAdapter.getConfig((GetConfigInput) msg);
            sendResponseToCore(reply, coreConnector, ofVersion, ((GetConfigInput) msg).getXid(), datapathId, moduleId);

        } else if (getImplementedInterface(msg).equals(GetFeaturesInput.class.getName())) {

            Future<RpcResult<GetFeaturesOutput>> reply = connectionAdapter.getFeatures((GetFeaturesInput) msg);
            sendResponseToCore(reply, coreConnector, ofVersion, ((GetFeaturesInput) msg).getXid(), datapathId,
                    moduleId);

        } else if (getImplementedInterface(msg).equals(GetQueueConfigInput.class.getName())) {

            Future<RpcResult<GetQueueConfigOutput>> reply = connectionAdapter.getQueueConfig((GetQueueConfigInput) msg);
            sendResponseToCore(reply, coreConnector, ofVersion, ((GetQueueConfigInput) msg).getXid(), datapathId,
                    moduleId);

        } else if (getImplementedInterface(msg).equals(GroupModInput.class.getName())) {

            connectionAdapter.groupMod((GroupModInput) msg);

        } else if (getImplementedInterface(msg).equals(HelloInput.class.getName())) {

            connectionAdapter.hello((HelloInput) msg);

        } else if (getImplementedInterface(msg).equals(MeterModInput.class.getName())) {

            connectionAdapter.meterMod((MeterModInput) msg);

        } else if (getImplementedInterface(msg).equals(MultipartRequestInput.class.getName())) {

            connectionAdapter.multipartRequest((MultipartRequestInput) msg);

        } else if (getImplementedInterface(msg).equals(PacketOutInput.class.getName())) {

            connectionAdapter.packetOut((PacketOutInput) msg);

        } else if (getImplementedInterface(msg).equals(PortModInput.class.getName())) {

            connectionAdapter.portMod((PortModInput) msg);

        } else if (getImplementedInterface(msg).equals(SetAsyncInput.class.getName())) {

            connectionAdapter.setAsync((SetAsyncInput) msg);

        } else if (getImplementedInterface(msg).equals(SetConfigInput.class.getName())) {

            connectionAdapter.setConfig((SetConfigInput) msg);

        } else if (getImplementedInterface(msg).equals(TableModInput.class.getName())) {
            connectionAdapter.tableMod((TableModInput) msg);

        } else if (getImplementedInterface(msg).equals(RoleRequestInput.class.getName())) {
            Future<RpcResult<RoleRequestOutput>> reply = connectionAdapter.roleRequest((RoleRequestInput) msg);
            sendResponseToCore(reply, coreConnector, ofVersion, ((RoleRequestInput) msg).getXid(), datapathId,
                    moduleId);
        } else {
            LOG.info("SHIM RELAY: Dataobject not recognized " + getImplementedInterface(msg));
        }

    }

    public String getImplementedInterface(DataObject message) {
        return message.getImplementedInterface().getName();
    }

    public <E extends DataObject> void sendResponseToCore(Future<RpcResult<E>> switchReply,
            final ZeroMQBaseConnector coreConnector, final short ofVersion, final long xId, final long datapathId,
            final int moduleId) {

        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(switchReply), new FutureCallback<RpcResult<E>>() {
            @Override
            public void onSuccess(RpcResult<E> rpcReply) {
                if (rpcReply.isSuccessful()) {
                    E result = rpcReply.getResult();

                    sendOpenFlowMessageToCore(coreConnector, result, ofVersion, xId, datapathId, moduleId);
                } else {
                    for (RpcError rpcError : rpcReply.getErrors()) {
                        LOG.info("SHIM RELAY: error in communication with switch: {}", rpcError.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.info("SHIM RELAY: failure on communication with switch");
            }
        });
    }
}
