/**
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *     Telefonica I+D
 */

package org.opendaylight.openflowplugin.pyretic.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

// OF imports
//import org.openflow.protocol.OFFlowMod;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6FlowLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.VlanCfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.CopyTtlInCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.CopyTtlOutCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecMplsTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopPbbActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushPbbActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetMplsTtlActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTtlActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.StripVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.controller.action._case.ControllerActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.copy.ttl.in._case.CopyTtlInBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.copy.ttl.out._case.CopyTtlOutBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.mpls.ttl._case.DecMplsTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.action._case.FloodActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.flood.all.action._case.FloodAllActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.hw.path.action._case.HwPathActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.loopback.action._case.LoopbackActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.pbb.action._case.PopPbbActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.mpls.action._case.PushMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.pbb.action._case.PushPbbActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.type.action._case.SetDlTypeActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.mpls.ttl.action._case.SetMplsTtlActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.next.hop.action._case.SetNextHopActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.tos.action._case.SetNwTosActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.ttl.action._case.SetNwTtlActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.queue.action._case.SetQueueActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.dst.action._case.SetTpDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.tp.src.action._case.SetTpSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.cfi.action._case.SetVlanCfiActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.pcp.action._case.SetVlanPcpActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.strip.vlan.action._case.StripVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.sw.path.action._case.SwPathActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.MeterCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.meter._case.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.metadata._case.WriteMetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ipv6.match.fields.Ipv6ExtHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ipv6.match.fields.Ipv6LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.TunnelIpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.protocol.match.fields.PbbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.node.error.service.rev140410.NodeErrorListener;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
/**
 * Created by Jennifer Hernández Bécares on 11/12/14.
 */
public class FlowModUtils {

    // Please refer to the following class from openflowplugin for examples:
    // https://github.com/opendaylight/openflowplugin/blob/
    // f478d4f38aac837545bf33c0d8745ce9cde3703c/test-provider/src/
    // main/java/org/opendaylight/openflowplugin/test/
    // OpenflowpluginTestCommandProvider.java


    // For example flows, refer to https://wiki.opendaylight.org/view/Editing_OpenDaylight_OpenFlow_Plugin:End_to_End_Flows:Example_Flows#Ethernet_Src_.26_Dest_Addresses.2C_IPv4_Src_.26_Dest_Addresses.2C_TCP_Src_.26_Dest_Ports.2C_IP_DSCP.2C_IP_ECN.2C_Input_Port
    private static final String ORIGINAL_FLOW_NAME = "Flow";
    private static AtomicLong flowIdInc = new AtomicLong();

    private FlowModUtils() {
        throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
    }

    /**
     * Extracts the source mac (srcmac) from the json match
     * @param json match
     * @return Returns the corresponding source mac string
     */
    private static String getSrcMac(JSONObject json) {
        JSONArray raw = (JSONArray) json.get("srcmac");
        String srcmacb = JSONArray2String(raw);
        return srcmacb;
    }

    /**
     * Extracts the destination mac (dstmac) from the json match
     * @param json match
     * @return Returns the corresponding dst mac string
     */
    private static String getDstMac(JSONObject json) {
        JSONArray raw = (JSONArray) json.get("dstmac");
        String dstmacb = JSONArray2String(raw);
        return dstmacb;
    }

    /**
     * Extracts the source ip (srcip) from the json match
     * @param json match
     * @return Returns the corresponding src ip string
     */
    private static String getSrcIp(JSONObject json) {
        JSONArray raw = (JSONArray) json.get("srcip");
        String srcipb = JSONArray2String(raw);
        return srcipb;
    }

    /**
     * Extracts the destination ip (dstip) from the json match
     * @param json match
     * @return Returns the corresponding dst ip string
     */
    private static String getDstIp(JSONObject json) {
        JSONArray raw = (JSONArray) json.get("dstip");
        String dstipb = JSONArray2String(raw);
        return dstipb;
    }

    /**
     * Creates a match when the ethernet type is 0 (only ethernet match)
     * It only sets the dst and src mac when they are relevant (not 0)
     * @param jsonmatch
     * @return
     */
    private static MatchBuilder createEthernetMatch(JSONObject jsonmatch) {

        // Source mac
        String srcmacb = getSrcMac(jsonmatch);
        System.out.println("source mac: " + srcmacb);
        // Destination mac
        String dstmacb = getDstMac(jsonmatch);
        System.out.println("Dst mac: " + dstmacb);

        if (dstmacb.equalsIgnoreCase("ff:ff:ff:ff:ff:ff"))
            return null;

        MatchBuilder match = new MatchBuilder();
        EthernetMatchBuilder ethmatch = new EthernetMatchBuilder(); // ethernettype
        EthernetTypeBuilder ethtype = new EthernetTypeBuilder();
        EtherType type = new EtherType(0x0800L);
        ethmatch.setEthernetType(ethtype.setType(type).build());

        // We create the mac address from the original match json
        // Note: Masks are ff:ff:ff:ff:ff:ff because it should be a perfect match
        EthernetDestinationBuilder ethdest = new EthernetDestinationBuilder(); // ethernet
        MacAddress macdest = new MacAddress(dstmacb);
        if (!dstmacb.equalsIgnoreCase("00:00:00:00:00:00")) {
            ethdest.setAddress(macdest);
            ethdest.setMask(new MacAddress("ff:ff:ff:ff:ff:ff"));
            ethmatch.setEthernetDestination(ethdest.build());
        }

        /*EthernetSourceBuilder ethsrc = new EthernetSourceBuilder();
        MacAddress macsrc = new MacAddress(srcmacb);
        if (!srcmacb.equalsIgnoreCase("00:00:00:00:00:00")) {
            ethsrc.setAddress(macsrc);
            ethsrc.setMask(new MacAddress("ff:ff:ff:ff:ff:ff"));
            ethmatch.setEthernetSource(ethsrc.build());
        }*/
        match.setEthernetMatch(ethmatch.build());

        return match;
    }

    /**
     * Creates a match when the ethernet type is ARP
     * It only sets the dst, src mac and dst, src ip when they are relevant (not 0)
     * @param jsonmatch
     * @return
     */
    private static MatchBuilder createArpMatch(JSONObject jsonmatch) {

        // Source mac
        String srcmacb = getSrcMac(jsonmatch);
        // Destination mac
        String dstmacb = getDstMac(jsonmatch);
        // Source ip
        String srcipb = getSrcIp(jsonmatch);
        // Destination ip
        String dstipb = getDstIp(jsonmatch);

        // We create the match from the dst, src mac
        MatchBuilder match = new MatchBuilder();
        EthernetMatchBuilder ethmatch = new EthernetMatchBuilder();
        MacAddress macdest = new MacAddress(dstmacb);
        MacAddress macsrc = new MacAddress(srcmacb);
        EthernetTypeBuilder ethtype = new EthernetTypeBuilder();
        EtherType type = new EtherType(0x0806L);
        ethmatch.setEthernetType(ethtype.setType(type).build());

        // ipv4 match
        Ipv4Prefix dstip = new Ipv4Prefix(dstipb);
        Ipv4Prefix srcip = new Ipv4Prefix(srcipb);

        // arp match
        ArpMatchBuilder arpmatch = new ArpMatchBuilder();
        ArpSourceHardwareAddressBuilder arpsrc = new ArpSourceHardwareAddressBuilder();
        arpsrc.setAddress(macsrc);
        arpsrc.setMask(new MacAddress("ff:ff:ff:ff:ff:ff"));
        ArpTargetHardwareAddressBuilder arpdst = new ArpTargetHardwareAddressBuilder();
        arpdst.setAddress(macdest);
        arpdst.setMask(new MacAddress("ff:ff:ff:ff:ff:ff"));
        arpmatch.setArpOp(2);

        arpmatch.setArpSourceHardwareAddress(arpsrc.build());
        arpmatch.setArpTargetHardwareAddress(arpdst.build());

        arpmatch.setArpSourceTransportAddress(srcip);
        arpmatch.setArpTargetTransportAddress(dstip);
        match.setEthernetMatch(ethmatch.build());
        match.setLayer3Match(arpmatch.build());
        return match;
    }

    /**
     * Creates a match when the ethernet type is IPv4
     * It only sets the dst, src mac and dst, src ip when they are relevant (not 0)
     * @param jsonmatch
     * @return
     */
    private static MatchBuilder createL3IPv4Match(JSONObject jsonmatch) {

        // Source mac
        String srcmacb = getSrcMac(jsonmatch);
        // Destination mac
        String dstmacb = getDstMac(jsonmatch);
        // Source ip
        String srcipb = getSrcIp(jsonmatch);
        // Destination ip
        String dstipb = getDstIp(jsonmatch);

        // We create the match
        MatchBuilder match = new MatchBuilder();
        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());

        // Set mac destination if different from 00:...:00
        if (!dstmacb.equalsIgnoreCase("00:00:00:00:00:00")) {
            EthernetDestinationBuilder ethdest = new EthernetDestinationBuilder(); // ethernet
            MacAddress macdest = new MacAddress(dstmacb);
            ethdest.setAddress(macdest);
            ethdest.setMask(new MacAddress("ff:ff:ff:ff:ff:ff"));
            eth.setEthernetDestination(ethdest.build());
        }

        // Set mac source if different from 00:...:00
       if (!srcmacb.equalsIgnoreCase("00:00:00:00:00:00")) {
            EthernetSourceBuilder ethsrc = new EthernetSourceBuilder();
            MacAddress macsrc = new MacAddress(srcmacb);
            ethsrc.setAddress(macsrc);
            ethsrc.setMask(new MacAddress("ff:ff:ff:ff:ff:ff"));
            eth.setEthernetSource(ethsrc.build());
        }

        // Sets ethernet match according to the already set source and destination macs
        match.setEthernetMatch(eth.build());

        // ipv4 match
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();

        // We only set the ips if they are relevant

        if (!dstipb.equalsIgnoreCase("0.0.0.0")) {
            Ipv4Prefix dstip = new Ipv4Prefix(dstipb);
            ipv4match.setIpv4Destination(dstip);
        }

        if (!srcipb.equalsIgnoreCase("0.0.0.0")) {
            Ipv4Prefix srcip = new Ipv4Prefix(srcipb);
            ipv4match.setIpv4Source(srcip);
        }

        match.setLayer3Match(ipv4match.build());
        return match;
    }

    /**
     * Creates LLDP match
     * @return
     */
    private static MatchBuilder createLLDPMatch() {
        MatchBuilder match = new MatchBuilder();
        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x88ccL));
        eth.setEthernetType(ethTypeBuilder.build());
        match.setEthernetMatch(eth.build());
        return match;
    }

    // Based on createTestFlow function from https://github.com/opendaylight/openflowplugin/blob/f478d4f38aac837545bf33c0d8745ce9cde3703c/test-provider/src/main/java/org/opendaylight/openflowplugin/test/OpenflowpluginTestCommandProvider.java
    public static FlowBuilder createFlowBuilder(JSONObject match, int priority, JSONObject actions) {
        FlowBuilder flow = new FlowBuilder();
        flow.setPriority(priority);

        if (match.containsKey("protocol")) {
            int protocol = Integer.parseInt(match.get("protocol").toString());

            if (protocol == 0 || protocol == 1 || protocol == 2) { // value 0 intends to ignore protocol
                // Ethernet type
                MatchBuilder newMatch = createEthernetMatch(match);
                if (newMatch == null)
                    return null;
                flow.setMatch(createEthernetMatch(match).build());
               /* if (Integer.parseInt(match.get("ethtype").toString()) == 0) {
                    System.out.println("type = 0");
                    flow.setMatch(createEthernetMatch(match).build());
                }
                // ARP (0x806)
                else if (Integer.parseInt(match.get("ethtype").toString()) == 2054) {
                    System.out.println("type = 2054 (ARP)");
                    flow.setMatch(createArpMatch(match).build());
                }
                // IP (0x800)
                else if (Integer.parseInt(match.get("ethtype").toString()) == 2048) {
                    System.out.println("type = IP (2048)");
                    flow.setMatch(createL3IPv4Match(match).build());
                }
                // IPv6 (0x86dd)
                else if (Integer.parseInt(match.get("ethtype").toString()) == 34525) {
                    System.out.println("type = IPv6");
                    // not handled yet
                }
                // LLDP (0x88cc)
                else if (Integer.parseInt(match.get("ethtype").toString()) == 35020) {
                    System.out.println("type = 35020 (LLDP)");
                    flow.setMatch(createLLDPMatch().build());
                }*/
            }
            else if (protocol == 6) {
                // TCP + IPv4
                // not handled yet
            }
            else if (protocol == 17) {
                // UDP + IPv4
                // not handled yet
            }
            else return null;

            // Create and set the actions here
            //String switc = match.get("switch").toString();
            //String inport = match.get("inport").toString();
            if (actions.containsKey("outport")) {
                String outport = actions.get("outport").toString();
                // TODO -> Only outport action handled
                flow.setInstructions(createOutputInstructions(outport).build()); // We indicate where to send the flow

                // create the flow
                BigInteger value = new BigInteger("10", 10); // FIXME test value
                flow.setCookie(new FlowCookie(value));
                flow.setCookieMask(new FlowCookie(value));
                if (match.containsKey("hard_timeout"))
                    flow.setHardTimeout(Integer.parseInt(match.get("hard_timeout").toString()));
                else
                    flow.setHardTimeout(0);
                if (match.containsKey("idle_timeout"))
                    flow.setIdleTimeout(Integer.parseInt(match.get("idle_timeout").toString()));
                else
                    flow.setIdleTimeout(0);
                flow.setInstallHw(false);
                flow.setStrict(false);
                flow.setContainerName(null);
                flow.setFlags(new FlowModFlags(false, false, false, false, true));

                FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
                flow.setId(flowId);
                short tid = 0;
                flow.setTableId(tid);
                FlowKey key = new FlowKey(flowId);
                flow.setKey(key);

                flow.setOutPort(BigInteger.valueOf(Long.parseLong(outport)));
                flow.setFlowName(ORIGINAL_FLOW_NAME + " number " + flowIdInc);
                return flow;
            }
            else {
                // TODO add different types of actions
                System.out.println("Unidentified action arrived: " + actions.keySet().toString());
                return null;
            }
        }
        else
            return null;
    }

    // TODO there are two different types of createOutputInstructions on the reference...
    // only one handled yet
    private static InstructionsBuilder createOutputInstructions(String outport) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        // The following creates an outport to switch port
        // For different actions create a *ActionBuilder instead


        OutputActionBuilder output = new OutputActionBuilder();
        Uri value = new Uri(outport); // FIXME: outport or switch? page 58 of openflow-spec-v1.3.2.pdf
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);
        return isb;
    }


    private static String JSONArray2String(JSONArray array) {
        StringBuffer result = new StringBuffer();
        for (int i=0;i<array.size();i++) {
            Long lh= (Long)array.get(i);
            result.append(Character.toChars(lh.intValue()));
        }
        return result.toString();
    }
}
