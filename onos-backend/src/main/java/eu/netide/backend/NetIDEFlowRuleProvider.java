/*
 *  Copyright (c) 2016, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 *  Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 *  Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 *  Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors: Antonio Marsico (antonio.marsico@create-net.org)
 */

package eu.netide.backend;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import eu.netide.backend.util.FlowEntryBuilder;
import eu.netide.backend.util.FlowModBuilder;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by antonio on 04/10/16.
 */
public class NetIDEFlowRuleProvider extends AbstractProvider implements FlowRuleProvider {

    private FlowRuleProviderService flowRuleProviderService;
    private NetIDEDeviceProvider deviceProvider;
    private NetIDEBackendController backendController;
    private IModuleHandler moduleHandler;
    private ApplicationService applicationService;
    private DriverService driverService;
    Logger log = getLogger(getClass());
    /**
     * Creates a provider with the supplied identifier.
     */
    protected NetIDEFlowRuleProvider() {
        super(new ProviderId("of", "eu.netide.provider.openflow"));
    }

    public void setFlowRuleProviderService(FlowRuleProviderService service) {
        this.flowRuleProviderService = service;
    }

    public void setDeviceProvider(NetIDEDeviceProvider deviceProvider) {
        this.deviceProvider = deviceProvider;
    }

    public void setBackendController(NetIDEBackendController backendController) {
        this.backendController = backendController;
    }

    public void setModuleHandler(IModuleHandler moduleHandler) {
        this.moduleHandler = moduleHandler;
    }
    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    public void setDriverService(DriverService driverService) {
        this.driverService = driverService;
    }

    public void notifyFlowRemoved(DeviceId deviceId, OFFlowRemoved msg) {

        FlowEntry fr = new FlowEntryBuilder(deviceId, msg, driverService).build();
        List<FlowEntry> entries = Lists.newArrayList(fr);
        //flowRuleProviderService.flowRemoved(fr);
        //flowRuleProviderService.pushFlowMetrics(deviceId, entries);
    }

    public void notifyStatistics (DeviceId deviceId, OFFlowStatsReply replies) {
        List<FlowEntry> flowEntries = replies.getEntries().stream()
                .map(entry -> new FlowEntryBuilder(deviceId, entry, driverService).build())
                .collect(Collectors.toList());
        flowRuleProviderService.pushFlowMetrics(deviceId, flowEntries);
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            applyRule(flowRule);
        }
    }
    private void applyRule(FlowRule flowRule) {

        Dpid dpid = Dpid.dpid(flowRule.deviceId().uri());
        OFFactory factory = deviceProvider.getSwitchOFFactory(dpid);
        String applicationName = BackendLayer.MODULE_NAME;
        Set<Application> runningApplications =  applicationService.getApplications();
        for (Application application : runningApplications) {
            if(application.id().id() == flowRule.appId()) {
                applicationName = application.id().name();
            }
        }

        if (factory == null) {
            return;
        }
        OFFlowMod flowMod = FlowModBuilder.builder(flowRule, factory,
                                                Optional.of((long) BackendLayer.getXId()), Optional.empty()).buildFlowAdd();

        int moduleId = moduleHandler.getModuleId(applicationName);

        backendController.sendOpenFlowMessageToCore(flowMod, (int)flowMod.getXid(), dpid.value(), moduleId);

        //Statistics update
        /*List<OFFlowStatsEntry> flowStatsEntries = Lists.newArrayList();
        OFFlowStatsEntry.Builder ofFlowStatsEntryBuilder = factory.buildFlowStatsEntry()
                .setMatch(flowMod.getMatch())
                .setInstructions(flowMod.getInstructions())
                .setDurationSec(5)
                .setIdleTimeout(flowMod.getIdleTimeout())
                .setPriority(flowMod.getPriority())
                .setCookie(flowMod.getCookie());
        if (factory.getVersion() == OFVersion.OF_13) {
            ofFlowStatsEntryBuilder.setTableId(flowMod.getTableId());
        }
        flowStatsEntries.add(ofFlowStatsEntryBuilder.build());

        OFFlowStatsReply statsReply = factory.buildFlowStatsReply()
                .setEntries(flowStatsEntries)
                .build();*/

        //notifyStatistics(flowRule.deviceId(), statsReply);
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            removeRule(flowRule);
        }
    }

    private void removeRule(FlowRule flowRule) {

        Dpid dpid = Dpid.dpid(flowRule.deviceId().uri());
        OFFactory factory = deviceProvider.getSwitchOFFactory(dpid);
        String applicationName = BackendLayer.MODULE_NAME;
        Set<Application> runningApplications =  applicationService.getApplications();
        for (Application application : runningApplications) {
            if(application.id().id() == flowRule.appId()) {
                applicationName = application.id().name();
            }
        }

        if (factory == null) {
            return;
        }
        OFFlowMod flowMod = FlowModBuilder.builder(flowRule, factory,
                                                   Optional.empty(), Optional.empty()).buildFlowDel();
        int moduleId = moduleHandler.getModuleId(applicationName);

        backendController.sendOpenFlowMessageToCore(flowMod, (int)flowMod.getXid(), dpid.value(), moduleId);
    }


    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {

    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {

        Dpid dpid = Dpid.dpid(batch.deviceId().uri());
        OFFactory factory = deviceProvider.getSwitchOFFactory(dpid);
        // If switch no longer exists, simply return.
        if (factory == null) {
            Set<FlowRule> failures = ImmutableSet.copyOf(Lists.transform(batch.getOperations(), e -> e.target()));
            flowRuleProviderService.batchOperationCompleted(batch.id(),
                                                    new CompletedBatchOperation(false, failures, batch.deviceId()));
            return;
        }

        String applicationName = BackendLayer.MODULE_NAME;
        OFFlowMod mod;
        OFFlowStatsReply statsReply;
        List<OFFlowStatsEntry> flowStatsEntries = Lists.newArrayList();
        for (FlowRuleBatchEntry fbe : batch.getOperations()) {

            FlowModBuilder builder = FlowModBuilder.builder(fbe.target(), factory,
                                                            Optional.of(batch.id()), Optional.of(driverService));
            switch (fbe.operator()) {
                case ADD:
                    mod = builder.buildFlowAdd();
                    //Statistics update
                    OFFlowStatsEntry.Builder ofFlowStatsEntryBuilder = factory.buildFlowStatsEntry()
                            .setMatch(mod.getMatch())
                            .setDurationSec(1)
                            .setDurationNsec(10)
                            .setIdleTimeout(mod.getIdleTimeout())
                            .setPriority(mod.getPriority())
                            .setCookie(mod.getCookie());
                    if (factory.getVersion() == OFVersion.OF_13) {
                        ofFlowStatsEntryBuilder.setTableId(mod.getTableId());
                        ofFlowStatsEntryBuilder.setInstructions(mod.getInstructions());
                    } else {
                        ofFlowStatsEntryBuilder.setActions(mod.getActions());
                    }
                    flowStatsEntries.add(ofFlowStatsEntryBuilder.build());

                    break;
                case REMOVE:
                    mod = builder.buildFlowDel();
                    //Statistics update
                    OFFlowRemoved.Builder ofFlowremoved = factory.buildFlowRemoved()
                            .setMatch(mod.getMatch())
                            .setDurationSec(10)
                            .setIdleTimeout(mod.getIdleTimeout())
                            .setPriority(mod.getPriority())
                            .setCookie(mod.getCookie());
                    if (factory.getVersion() == OFVersion.OF_13) {
                        ofFlowremoved.setTableId(mod.getTableId());
                    }
                    notifyFlowRemoved(batch.deviceId(), ofFlowremoved.build());
                    break;
                case MODIFY:
                    mod = builder.buildFlowMod();
                    break;
                default:
                    log.error("Unsupported batch operation {}; skipping flowmod {}",
                              fbe.operator(), fbe);
                    continue;
            }

            Set<Application> runningApplications =  applicationService.getApplications();
            for (Application application : runningApplications) {
                if(application.id().id() == fbe.target().appId()) {
                    applicationName = application.id().name();
                }
            }

            int moduleId = moduleHandler.getModuleId(applicationName);

            backendController.sendOpenFlowMessageToCore(mod, backendController.getLastXid().get(), dpid.value(), moduleId);
        }

        flowRuleProviderService.batchOperationCompleted(batch.id(),
                                                        new CompletedBatchOperation(true, Collections.emptySet(), batch.deviceId()));

        //statsReply = factory.buildFlowStatsReply()
        //        .setEntries(flowStatsEntries)
        //        .build();
        //if (flowStatsEntries.size() > 0) {
        //    notifyStatistics(batch.deviceId(), statsReply);
        //}

    }
}
