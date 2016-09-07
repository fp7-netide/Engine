# Copyright (C) 2011 Nippon Telegraph and Telephone Corporation.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging

from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_3
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.lib.packet import ether_types

logger = logging.getLogger('firewall')
logger.setLevel(logging.DEBUG)

PROTO_TCP = 6
PROTO_UDP = 17
ETH_ARP = 0x0806
ETH_IP = 0x0800
PORT_DNS = 53
PORT_WEB = 80

HOST_WEB = "10.0.0.10"
FW_OUTPORT = 1
FW_INPORT = 2

class Firewall(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_3.OFP_VERSION]

    def __init__(self, *args, **kwargs):
        super(Firewall, self).__init__(*args, **kwargs)
        self.mac_to_port = {}
        self.stateless_FW_configured = False

    def ipv4_to_int(self, ip):
        o = map(int, ip.split('.'))
        res = (16777216 * o[0]) + (65536 * o[1]) + (256 * o[2]) + o[3]
        return res

    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def switch_features_handler(self, ev):
        datapath = ev.msg.datapath
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        # install table-miss flow entry
        #
        # We specify NO BUFFER to max_len of the output action due to
        # OVS bug. At this moment, if we specify a lesser number, e.g.,
        # 128, OVS will send Packet-In with invalid buffer_id and
        # truncated packet data. In that case, we cannot output packets
        # correctly.  The bug has been fixed in OVS v2.1.0.
        match = parser.OFPMatch()
        actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER,
                                          ofproto.OFPCML_NO_BUFFER)]
        self.add_flow(datapath, 0, match, actions)

        if  self.stateless_FW_configured == False:
            self.Configure_stateless_FW(datapath)
            self.stateless_FW_configured = True

    def add_flow(self, datapath, priority, match, actions, idle_to=0, hard_to=0, buffer_id=None):
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        inst = [parser.OFPInstructionActions(ofproto.OFPIT_APPLY_ACTIONS,
                                             actions)]
        if buffer_id:
            mod = parser.OFPFlowMod(datapath=datapath, buffer_id=buffer_id,
                                    priority=priority, match=match,
                                    instructions=inst6)
        else:
            mod = parser.OFPFlowMod(datapath=datapath, priority=priority,
                                    match=match, instructions=inst, idle_timeout=idle_to, hard_timeout=hard_to)

        datapath.send_msg(mod)

    def forwardPacket(self, msg, outPort):
        # Does not install a rule. Just forwards this packet.
        datapath = msg.datapath
        in_port = msg.match['in_port']
        parser = datapath.ofproto_parser
        data = None

        if msg.buffer_id is not None:
            if msg.buffer_id == datapath.ofproto.OFP_NO_BUFFER:
                data = msg.data
            po_actions = [parser.OFPActionOutput(outPort)]
            pkt_out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port, actions=po_actions, data=data)
            datapath.send_msg(pkt_out)

    # Static rules for the web and dns services
    def Configure_stateless_FW(self,datapath):
        parser = datapath.ofproto_parser

        actions = [parser.OFPActionOutput(FW_INPORT)]

        match = parser.OFPMatch(in_port=FW_OUTPORT,eth_type = ETH_IP, ipv4_dst = self.ipv4_to_int(HOST_WEB),
		   ip_proto = PROTO_TCP, tcp_dst = PORT_WEB)
        self.add_flow(datapath, 2, match, actions, 8, 0)

        actions = [parser.OFPActionOutput(FW_OUTPORT)]
        match = parser.OFPMatch(in_port=FW_INPORT,eth_type = ETH_IP, ip_proto = PROTO_TCP, tcp_dst = PORT_WEB)
        self.add_flow(datapath, 2, match, actions, 8, 0)

    def Configure_stateful_FW(self, msg):
        pkt = packet.Packet(msg.data)
        datapath = msg.datapath
        parser = datapath.ofproto_parser
        ofproto = datapath.ofproto
        in_port = msg.match['in_port']

        eth = pkt.get_protocols(ethernet.ethernet)[0]
        hwdst = eth.dst
        hwsrc = eth.src
        global COUNTER

        if eth.ethertype == ether_types.ETH_TYPE_LLDP:
            # ignore lldp packet
            return

        # Forward all arp
        if eth.ethertype == ether_types.ETH_TYPE_ARP:
            logger.debug("Received ARP packet, forwarding it via Packet out: %s" % repr(pkt))
            if in_port == FW_INPORT:
                self.forwardPacket(msg, FW_OUTPORT)
            if in_port == FW_OUTPORT:
                self.forwardPacket(msg, FW_INPORT)
                
        # Forward packets from inside to outside and also install the reverse rule with idle_to=8 sec
        elif in_port == FW_INPORT:
            logger.debug("Got packet from inside to outside, allowing it (fwd+flow mod): %s" % repr(pkt))
            match = parser.OFPMatch(in_port=FW_INPORT, eth_type = ETH_IP, eth_src=hwsrc, eth_dst=hwdst)
            actions = [parser.OFPActionOutput(FW_OUTPORT)]
            self.add_flow(datapath, 10, match, actions, 8, 0)

            match = parser.OFPMatch(in_port=FW_OUTPORT, eth_type = ETH_IP, eth_src=hwdst, eth_dst=hwsrc)
            actions = [parser.OFPActionOutput(FW_INPORT)]
            self.add_flow(datapath, 10, match, actions, 8, 0)

            # forward the packet
            self.forwardPacket(msg, FW_OUTPORT)
            print(">>> FW: FORWARD PACKET from %s to port 1" % (in_port))
            
        elif in_port == FW_OUTPORT:
            logger.debug("Droping packet from in_port %d: %s" % (in_port,repr(pkt)))
            match = parser.OFPMatch(in_port=FW_OUTPORT, eth_type = ETH_IP, eth_src=hwsrc, eth_dst=hwdst)
            actions = []
            self.add_flow(datapath, 5, match, actions, 8, 0)


    # PacketIn handler for reactive actions
    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def _packet_in_handler(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        self.Configure_stateful_FW(msg)
