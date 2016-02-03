/*
 * Copyright (c) 2015 NetIDE Consortium and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netide.shim;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionReadyListener;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.openflowplugin.api.openflow.md.core.IMDMessageTranslator;
import org.opendaylight.openflowplugin.api.openflow.md.core.NotificationQueueWrapper;
import org.opendaylight.openflowplugin.api.openflow.md.core.TranslatorKey;
import org.opendaylight.openflowplugin.openflow.md.core.session.SessionContextOFImpl;
import org.opendaylight.openflowplugin.openflow.md.core.translator.ErrorTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.ErrorV10Translator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.ExperimenterTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.FeaturesV10ToNodeConnectorUpdatedTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.FlowRemovedTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.MultiPartMessageDescToNodeUpdatedTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.MultiPartReplyPortToNodeConnectorUpdatedTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.MultipartReplyTableFeaturesToTableUpdatedTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.MultipartReplyTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.NotificationPlainTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.PacketInTranslator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.PacketInV10Translator;
import org.opendaylight.openflowplugin.openflow.md.core.translator.PortStatusMessageToNodeConnectorUpdatedTranslator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoReplyInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.EchoRequestMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ErrorMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.ExperimenterMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowRemovedMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.GetFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.HelloMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartReplyMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.OfHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.OpenflowProtocolListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PacketInMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.PortStatusMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.DisconnectEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SwitchIdleEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SystemNotificationsListener;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShimMessageListener
        implements OpenflowProtocolListener, SystemNotificationsListener, ConnectionReadyListener {

    private static final Logger LOG = LoggerFactory.getLogger(ShimMessageListener.class);
    private ConnectionAdaptersRegistry connectionRegistry;
    public static final Long DEFAULT_XID = 0x01020304L;
    private ZeroMQBaseConnector coreConnector;
    private ConnectionAdapter switchConnection;
    private IHandshakeListener handshakeListener;
    private ShimRelay shimRelay;
    private ShimSwitchConnectionHandlerImpl connectionHandler;
    private ConcurrentMap<TranslatorKey, Collection<IMDMessageTranslator<OfHeader, List<DataObject>>>> messageTranslators;
    final private int OF10 = OFConstants.OFP_VERSION_1_0;
    final private int OF13 = OFConstants.OFP_VERSION_1_3;
    SessionContextOFImpl sc;
    private NotificationPublishService notificationProviderService;

    public ShimMessageListener(ZeroMQBaseConnector connector, ConnectionAdapter switchConnection, ShimRelay _shimRelay,
            ShimSwitchConnectionHandlerImpl handler, NotificationPublishService _notificationProviderService) {
        this.coreConnector = connector;
        this.switchConnection = switchConnection;
        this.shimRelay = _shimRelay;
        this.connectionHandler = handler;
        notificationProviderService = _notificationProviderService;

        messageTranslators = new ConcurrentHashMap<>();

        addMessageTranslator(ErrorMessage.class, OF10, new ErrorV10Translator());
        addMessageTranslator(ErrorMessage.class, OF13, new ErrorTranslator());
        addMessageTranslator(FlowRemovedMessage.class, OF10, new FlowRemovedTranslator());
        addMessageTranslator(FlowRemovedMessage.class, OF13, new FlowRemovedTranslator());
        addMessageTranslator(PacketInMessage.class, OF10, new PacketInV10Translator());
        addMessageTranslator(PacketInMessage.class, OF13, new PacketInTranslator());
        addMessageTranslator(PortStatusMessage.class, OF10, new PortStatusMessageToNodeConnectorUpdatedTranslator());
        addMessageTranslator(PortStatusMessage.class, OF13, new PortStatusMessageToNodeConnectorUpdatedTranslator());
        addMessageTranslator(MultipartReplyMessage.class, OF13,
                new MultiPartReplyPortToNodeConnectorUpdatedTranslator());
        addMessageTranslator(MultipartReplyMessage.class, OF10, new MultiPartMessageDescToNodeUpdatedTranslator());
        addMessageTranslator(MultipartReplyMessage.class, OF13, new MultiPartMessageDescToNodeUpdatedTranslator());
        addMessageTranslator(ExperimenterMessage.class, OF10, new ExperimenterTranslator());
        addMessageTranslator(MultipartReplyMessage.class, OF10, new MultipartReplyTranslator());
        addMessageTranslator(MultipartReplyMessage.class, OF13, new MultipartReplyTranslator());
        addMessageTranslator(MultipartReplyMessage.class, OF13,
                new MultipartReplyTableFeaturesToTableUpdatedTranslator());
        addMessageTranslator(GetFeaturesOutput.class, OF10, new FeaturesV10ToNodeConnectorUpdatedTranslator());
        addMessageTranslator(NotificationQueueWrapper.class, OF10, new NotificationPlainTranslator());
        addMessageTranslator(NotificationQueueWrapper.class, OF13, new NotificationPlainTranslator());
        sc = new SessionContextOFImpl();
    }

    public void initSession(GetFeaturesOutput featuresOutput) {
        if (featuresOutput != null) {
            sc.setFeatures(featuresOutput);
            ShimConductor cond = new ShimConductor();
            cond.setVersion(featuresOutput.getVersion());
            sc.setPrimaryConductor(cond);
        }
    }

    public void addMessageTranslator(final Class<? extends DataObject> messageType, final int version,
            final IMDMessageTranslator<OfHeader, List<DataObject>> translator) {
        TranslatorKey tKey = new TranslatorKey(version, messageType.getName());

        Collection<IMDMessageTranslator<OfHeader, List<DataObject>>> existingValues = messageTranslators.get(tKey);
        if (existingValues == null) {
            existingValues = new LinkedHashSet<>();
            messageTranslators.put(tKey, existingValues);
        }
        existingValues.add(translator);
        LOG.debug("{} is now translated by {}", messageType, translator);
    }

    public void registerConnectionAdaptersRegistry(ConnectionAdaptersRegistry connectionRegistry) {
        this.connectionRegistry = connectionRegistry;
    }

    public void registerHandshakeListener(IHandshakeListener listener) {
        this.handshakeListener = listener;
    }

    private void sendNotification(OfHeader message, String messageClass) {
        TranslatorKey key = new TranslatorKey(message.getVersion(), messageClass);

        if (messageTranslators.containsKey(key)) {
            for (IMDMessageTranslator<OfHeader, List<DataObject>> translator : messageTranslators.get(key)) {
                List<DataObject> list = translator.translate(null, sc, message);
                for (DataObject dataObj : list) {
                    notificationProviderService.offerNotification((Notification) dataObj);

                }
            }
        }

    }

    /// OpenflowProtocolListener methods/////
    @Override
    public void onEchoRequestMessage(EchoRequestMessage arg0) {

        BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
        if (datapathId == null) {
            EchoReplyInputBuilder builder = new EchoReplyInputBuilder();
            builder.setVersion(arg0.getVersion());
            builder.setXid(arg0.getXid() + 1L);
            builder.setData(arg0.getData());
            this.switchConnection.echoReply(builder.build());
            connectionHandler.sendGetFeaturesOuputToCore(arg0.getVersion(), 0, switchConnection);
        } else {
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }
    }

    @Override
    public void onErrorMessage(ErrorMessage arg0) {
        if (connectionRegistry.getFeaturesOutput(switchConnection) != null) {
            BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
            initSession(connectionRegistry.getFeaturesOutput(switchConnection));
            sendNotification(arg0, arg0.getImplementedInterface().getName());
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }
    }

    @Override
    public void onExperimenterMessage(ExperimenterMessage arg0) {
        if (connectionRegistry.getFeaturesOutput(switchConnection) != null) {
            BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
            initSession(connectionRegistry.getFeaturesOutput(switchConnection));
            sendNotification(arg0, arg0.getImplementedInterface().getName());
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }
    }

    @Override
    public void onFlowRemovedMessage(FlowRemovedMessage arg0) {
        if (connectionRegistry.getFeaturesOutput(switchConnection) != null) {
            BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
            initSession(connectionRegistry.getFeaturesOutput(switchConnection));
            sendNotification(arg0, arg0.getImplementedInterface().getName());
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }
    }

    @Override
    public void onHelloMessage(HelloMessage arg0) {
        BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
        if (datapathId == null) {
            handshakeListener.onSwitchHelloMessage(arg0.getXid(), arg0.getVersion(), this.switchConnection);
        } else {
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }

    }

    @Override
    public void onMultipartReplyMessage(MultipartReplyMessage arg0) {
        if (connectionRegistry.getFeaturesOutput(switchConnection) != null) {
            BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
            initSession(connectionRegistry.getFeaturesOutput(switchConnection));
            sendNotification(arg0, arg0.getImplementedInterface().getName());
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }
    }

    @Override
    public void onPacketInMessage(PacketInMessage arg0) {
        if (connectionRegistry.getFeaturesOutput(switchConnection) != null) {
            BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
            initSession(connectionRegistry.getFeaturesOutput(switchConnection));
            sendNotification(arg0, arg0.getImplementedInterface().getName());
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }
    }

    @Override
    public void onPortStatusMessage(PortStatusMessage arg0) {
        if (connectionRegistry.getFeaturesOutput(switchConnection) != null) {
            BigInteger datapathId = this.connectionRegistry.getDatapathID(this.switchConnection);
            initSession(connectionRegistry.getFeaturesOutput(switchConnection));
            sendNotification(arg0, arg0.getImplementedInterface().getName());
            shimRelay.sendOpenFlowMessageToCore(coreConnector, arg0, arg0.getVersion(), arg0.getXid(),
                    datapathId.longValue(), 0);
        }
    }

    //// SystemNotificationsListener methods ////
    @Override
    public void onDisconnectEvent(DisconnectEvent arg0) {
        handshakeListener.onSwitchDisconnected(switchConnection);
        // this.connectionRegistry.removeConnectionAdapter(this.switchConnection);
    }

    @Override
    public void onSwitchIdleEvent(SwitchIdleEvent arg0) {

    }

    //// SystemNotificationsListener methods ////
    @Override
    public void onConnectionReady() {

    }
}
