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
/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.pyretic.Utils;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.controller.liblldp.LLDPTLV;
import org.opendaylight.controller.liblldp.LLDP;

/**
 * Created by Jennifer Hernández Bécares on 11/12/14.
 */
public class OutputUtils {

        private OutputUtils() {
            throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
        }

        public static String makePingFlowForNode(final String nodeId, final ProviderContext pc) {
            NodeBuilder nodeBuilder = createNodeBuilder(nodeId);
            FlowBuilder flowBuilder = createFlowBuilder(1235, null, "ping");
            DataBroker dataBroker = pc.<DataBroker>getSALService(DataBroker.class);
            ReadWriteTransaction modif = dataBroker.newReadWriteTransaction();
            InstanceIdentifier<Flow> path = InstanceIdentifier.<Nodes>builder(Nodes.class)
                    .<Node, NodeKey>child(Node.class, nodeBuilder.getKey())
                    .<FlowCapableNode>augmentation(FlowCapableNode.class)
                    .<Table, TableKey>child(Table.class, new TableKey(flowBuilder.getTableId()))
                    .<Flow, FlowKey>child(Flow.class, flowBuilder.getKey())
                    .build();
            modif.put(LogicalDatastoreType.CONFIGURATION, path, flowBuilder.build());
            CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modif.submit();
            final StringBuffer aggregator = new StringBuffer();
            Futures.addCallback(commitFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    aggregator.append("Status of Flow Data Loaded Transaction: succes ");
                }
                @Override
                public void onFailure(Throwable throwable) {
                    aggregator.append(throwable.getClass().getName());
                }
            });
            return aggregator.toString();
        }
        public static NodeRef createNodeRef(final String nodeId) {
            NodeKey key = new NodeKey(new NodeId(nodeId));
            InstanceIdentifier<Node> path = InstanceIdentifier.<Nodes>builder(Nodes.class)
                    .<Node, NodeKey>child(Node.class, key)
                    .toInstance();
            return new NodeRef(path);
        }
        public static NodeConnectorRef createNodeConnRef(final String nodeId, final String port) {
            StringBuilder sBuild = new StringBuilder(nodeId).append(':').append(port);
            NodeConnectorId _nodeConnectorId = new NodeConnectorId(sBuild.toString());
            NodeConnectorKey nConKey = new NodeConnectorKey(new NodeConnectorId(sBuild.toString()));
            InstanceIdentifier<NodeConnector> path = InstanceIdentifier.<Nodes>builder(Nodes.class)
                    .<Node, NodeKey>child(Node.class, new NodeKey(new NodeId(nodeId)))
                    .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nConKey)
                    .build();
            return new NodeConnectorRef(path);
        }
        private static NodeBuilder createNodeBuilder(final String nodeId) {
            NodeBuilder builder = new NodeBuilder();
            builder.setId(new NodeId(nodeId));
            builder.setKey(new NodeKey(builder.getId()));
            return builder;
        }
        public static FlowBuilder createFlowBuilder(final long flowId, final String tableId, final String flowName) {
            FlowBuilder fBuild = new FlowBuilder();
            fBuild.setMatch(new MatchBuilder().build());
            fBuild.setInstructions(createPingInstructionsBuilder().build());
            FlowKey key = new FlowKey(new FlowId(Long.toString(flowId)));
            fBuild.setBarrier(false);
// flow.setBufferId(new Long(12));
            final BigInteger value = BigInteger.valueOf(10);
            fBuild.setCookie(new FlowCookie(value));
            fBuild.setCookieMask(new FlowCookie(value));
            fBuild.setHardTimeout(0);
            fBuild.setIdleTimeout(0);
            fBuild.setInstallHw(false);
            fBuild.setStrict(false);
            fBuild.setContainerName(null);
            fBuild.setFlags(new FlowModFlags(false, false, false, false, false));
            fBuild.setId(new FlowId("12"));
            fBuild.setTableId(checkTableId(tableId));
            fBuild.setOutGroup(2L);
            fBuild.setOutPort(value);
            fBuild.setKey(key);
            fBuild.setPriority(2);
            fBuild.setFlowName(flowName);
            return fBuild;
        }
        private static InstructionsBuilder createPingInstructionsBuilder() {
            ArrayList<Action> aList = new ArrayList<Action>();
            ActionBuilder aBuild = new ActionBuilder();
            OutputActionBuilder output = new OutputActionBuilder();
            output.setMaxLength(56);
            output.setOutputNodeConnector(new Uri("CONTROLLER"));
            aBuild.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
            aBuild.setOrder(0);
            aBuild.setKey(new ActionKey(0));
            aList.add(aBuild.build());
            ApplyActionsBuilder asBuild = new ApplyActionsBuilder();
            asBuild.setAction(aList);
            InstructionBuilder iBuild = new InstructionBuilder();
            iBuild.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(asBuild.build()).build());
            iBuild.setOrder(0);
            iBuild.setKey(new InstructionKey(0));
            ArrayList<Instruction> instr = new ArrayList<Instruction>();
            instr.add(iBuild.build());
            return new InstructionsBuilder().setInstruction(instr);
        }
        private static short checkTableId(final String tableId) {
            try {
                return Short.parseShort(tableId);
            } catch (Exception ex) {
                return 2;
            }
        }


    /**
     * @param nodeId
     * @param payload
     * @param outPort
     * @param inPort
     * @return
     * */
        public synchronized static TransmitPacketInput createPacketOut(final String nodeId,
                                                          final byte[] payload,
                                                          final String outPort,
                                                          final String inPort) {
            ArrayList<Byte> list = new ArrayList<Byte>(40);

            for (byte b : payload) {
                list.add(b);
            }

            NodeRef ref = createNodeRef(nodeId);
            NodeConnectorRef nEgressConfRef = new NodeConnectorRef(createNodeConnRef(nodeId, outPort));
            NodeConnectorRef nIngressConRef = new NodeConnectorRef(createNodeConnRef(nodeId, inPort));
            TransmitPacketInputBuilder tPackBuilder = new TransmitPacketInputBuilder();
            final ArrayList<Byte> _converted_list = list;
            byte[] _primitive = ArrayUtils.toPrimitive(_converted_list.toArray(new Byte[0]));

            List<Action> actionList = new ArrayList<Action>();
            ActionBuilder ab = new ActionBuilder();
            OutputActionBuilder output = new OutputActionBuilder();
            output.setMaxLength(Integer.valueOf(0xffff));
            Uri value = new Uri(OutputPortValues.NORMAL.toString());
            output.setOutputNodeConnector(value);
            ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            actionList.add(ab.build());

            tPackBuilder.setConnectionCookie(null);
            tPackBuilder.setAction(actionList);
            tPackBuilder.setPayload(_primitive);
            tPackBuilder.setNode(ref);
            tPackBuilder.setEgress(nEgressConfRef);
            tPackBuilder.setIngress(nIngressConRef);
            tPackBuilder.setBufferId(Long.valueOf(0xffffffffL));

            return tPackBuilder.build();
        }

    /**
     * Receives a string with nibbles and creates an array of bytes
     * @param hexString
     * @return
     */
        public static final byte[] toByteArray (String hexString)
        {
            int arrLength = hexString.length() >> 1;
            byte buf[] = new byte[arrLength];

            for ( int ii = 0; ii < arrLength; ii++ )
            {
                int index = ii << 1;

                String l_digit = hexString.substring( index, index + 2 );
                buf[ii] = ( byte ) Integer.parseInt( l_digit, 16 );
            }

            return buf;
        }

     public static final String fromDecimalToHex (Long num)
     {
         int rem;

         // For storing result
         String str="";

         // Digits in hexadecimal number system
         char hex[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

         while(num>0)
         {
             rem=Integer.parseInt(num.toString())%16;
             str=hex[rem]+str;
             num=num/16;
         }
         while (str.length() < 2) str = "0" + str;
         return str;
     }
}
