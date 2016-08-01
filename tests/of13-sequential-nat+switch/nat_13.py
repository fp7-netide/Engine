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

from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_3
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4
from ryu.lib.packet import ether_types

INT_ADDR = "192.168.0."
EXT_ADDR = "10.0.0."
EXT_PORT = 2

def substract(a, b):
    return "".join(a.rsplit(b))

class NAT(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_3.OFP_VERSION]

    def __init__(self, *args, **kwargs):
        super(NAT, self).__init__(*args, **kwargs)
        self.mac_to_port = {}
        self.nat_table = {}

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

    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, MAIN_DISPATCHER)
    def tests_features_handler(self, ev):
        datapath = ev.msg.datapath
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        print "SWITCH features_reply: ", ev.msg

    def send_features_request(self, datapath):
        ofp_parser = datapath.ofproto_parser

        req = ofp_parser.OFPFeaturesRequest(datapath)
        req.xid = 3333
        print "SWITCH feature request: ", req
        datapath.send_msg(req)

    def add_flow(self, datapath, priority, match, actions, buffer_id=None):
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser

        inst = [parser.OFPInstructionActions(ofproto.OFPIT_APPLY_ACTIONS,
                                             actions)]
        if buffer_id:
            mod = parser.OFPFlowMod(datapath=datapath, buffer_id=buffer_id,
                                    priority=priority, match=match,
                                    instructions=inst, idle_timeout=5, hard_timeout=0)
        else:
            mod = parser.OFPFlowMod(datapath=datapath, priority=priority,
                                    match=match, instructions=inst, idle_timeout=5, hard_timeout=0)
        datapath.send_msg(mod)

    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def _packet_in_handler(self, ev):
        # If you hit this you might want to increase
        # the "miss_send_length" of your switch
        if ev.msg.msg_len < ev.msg.total_len:
            self.logger.debug("packet truncated: only %s of %s bytes",
                              ev.msg.msg_len, ev.msg.total_len)
        msg = ev.msg

        datapath = msg.datapath
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        in_port = msg.match['in_port']
        global COUNTER

        pkt = packet.Packet(msg.data)
        eth = pkt.get_protocols(ethernet.ethernet)[0]
        ipaddr = pkt.get_protocol(ipv4.ipv4)

        if eth.ethertype == ether_types.ETH_TYPE_LLDP:
            # ignore lldp packet
            return
        dst = eth.dst
        src = eth.src


        if ipaddr is not None:
            ipdst = ipaddr.dst
            ipsrc = ipaddr.src
            actions = []

            # we do not handle LAN traffic
            if INT_ADDR in ipsrc and INT_ADDR in ipdst:
                return

            print "source address: ", ipsrc
            if INT_ADDR in ipsrc: #from inside to outside
                new_ipsrc = EXT_ADDR + substract(ipsrc,INT_ADDR)
                self.nat_table[ipsrc] = in_port
                out_port = EXT_PORT
                actions.append(datapath.ofproto_parser.OFPActionSetField(ipv4_src=new_ipsrc))
                actions.append(datapath.ofproto_parser.OFPActionOutput(out_port))
            else:                 #from outside to inside
                new_ipdst = INT_ADDR + substract(ipdst,EXT_ADDR)
                out_port = self.nat_table[new_ipdst]
                actions.append(datapath.ofproto_parser.OFPActionSetField(ipv4_dst=new_ipdst))
                actions.append(datapath.ofproto_parser.OFPActionOutput(out_port))



            data = None
            if msg.buffer_id == ofproto.OFP_NO_BUFFER:
                data = msg.data

            out = parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id,
                                      in_port=in_port, actions=actions, data=data)

            print "packet_out: ", out

            datapath.send_msg(out)
