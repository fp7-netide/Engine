import logging
import struct

from ryu.base import app_manager
from ryu.controller import mac_to_port
from ryu.controller import ofp_event
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_parser
from ryu.ofproto import ofproto_common
from ryu.ofproto import ofproto_v1_0
from ryu.ofproto import ofproto_v1_0_parser
from ryu.lib.mac import haddr_to_bin
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4
from config import *

PROTO_TCP = 6
PROTO_UDP = 17
ETH_ARP = 0x0806
ETH_IP = 0x0800
#PORT_DNS = 53
#PORT_WEB = 80

#HOST_WEB = "10.0.0.10"

FW_OUTPORT = 1
FW_INPORT = 2

class Firewall(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_0.OFP_VERSION]

    def __init__(self, *args, **kwargs):
        super(Firewall, self).__init__(*args, **kwargs)
        self.mac_to_port = {}
        self.knownMACs = {}
        self.ofproto = ofproto_v1_0
        self.ofproto_parser = ofproto_v1_0_parser
        self.stateless_FW_configured = False
        
    def ipv4_to_int(self, ip):
        o = map(int, ip.split('.'))
        res = (16777216 * o[0]) + (65536 * o[1]) + (256 * o[2]) + o[3]
        return res

    def add_flow(self, datapath, match, actions, idle_to, hard_to):
        ofproto = datapath.ofproto

        mod = datapath.ofproto_parser.OFPFlowMod(
            datapath=datapath, match=match, cookie=0,
            command=ofproto.OFPFC_ADD, idle_timeout=idle_to, hard_timeout=hard_to,
            priority=ofproto.OFP_DEFAULT_PRIORITY,
            flags=ofproto.OFPFF_SEND_FLOW_REM, actions=actions)
        datapath.send_msg(mod)
        
    def forwardPacket(self, msg, outPort):
        # Does not install a rule. Just forwards this packet.
        datapath=msg.datapath        
        if msg.buffer_id is not None:
            po_actions = [datapath.ofproto_parser.OFPActionOutput(outPort)]
            pkt_out = datapath.ofproto_parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=msg.in_port, actions=po_actions)
            datapath.send_msg(pkt_out)
        
    # Static rules for the web and dns services
    def Configure_stateless_FW(self,datapath):
    
        actions = [self.ofproto_parser.OFPActionOutput(FW_INPORT)]
        match = self.ofproto_parser.OFPMatch(in_port=FW_OUTPORT,dl_type = ETH_IP, nw_dst = self.ipv4_to_int(HOST_WEB), 
           nw_proto = PROTO_TCP, tp_dst = PORT_WEB)
        self.add_flow(datapath, match, actions, 0, 0)
        
        actions = [self.ofproto_parser.OFPActionOutput(FW_OUTPORT)]
        match = self.ofproto_parser.OFPMatch(in_port=FW_INPORT,dl_type = ETH_IP, nw_proto = PROTO_TCP, tp_src = PORT_WEB)
        self.add_flow(datapath, match, actions, 0, 0)

    def Configure_stateful_FW(self, msg):
        pkt = packet.Packet(msg.data)
        datapath = msg.datapath
        
        eth = pkt.get_protocol(ethernet.ethernet)
        hwdst = eth.dst
        hwsrc = eth.src
        
        # Forward all arp
        if eth.ethertype == ETH_ARP:
            if msg.in_port == 2:
                self.forwardPacket(msg, 1)
            if msg.in_port == 1:
                self.forwardPacket(msg, 2)        
        # Forward packets from inside to outside and also install the reverse rule with idle_to=5 sec
        elif msg.in_port == 2:
            match = datapath.ofproto_parser.OFPMatch(in_port=FW_INPORT, dl_type = ETH_IP, dl_src=haddr_to_bin(hwsrc), dl_dst=haddr_to_bin(hwdst))
            actions = [datapath.ofproto_parser.OFPActionOutput(FW_OUTPORT)]
            self.add_flow(datapath, match, actions, STATEFUL_IDLE_TIMEOUT, 0)
            
            match = datapath.ofproto_parser.OFPMatch(in_port=FW_OUTPORT, dl_type = ETH_IP, dl_src=haddr_to_bin(hwdst), dl_dst=haddr_to_bin(hwsrc))
            actions = [datapath.ofproto_parser.OFPActionOutput(FW_INPORT)]
            self.add_flow(datapath, match, actions, STATEFUL_IDLE_TIMEOUT, 0)

            
            # forward the packet
            self.forwardPacket(msg, 1)  

    # Feature reply handler: used to install proactive actions
    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, MAIN_DISPATCHER)
    def _switch_features_handler(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        
        if self.stateless_FW_configured == False:
            self.Configure_stateless_FW(datapath)
            self.stateless_FW_configured = True
    
    # PacketIn handler for reactive actions
    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def _packet_in_handler(self, ev):
        msg = ev.msg
        datapath = msg.datapath

        pkt = packet.Packet(msg.data)
        self.Configure_stateful_FW(msg)




    @set_ev_cls(ofp_event.EventOFPPortStatus, MAIN_DISPATCHER)
    def _port_status_handler(self, ev):
        msg = ev.msg
        reason = msg.reason
        port_no = msg.desc.port_no

        ofproto = msg.datapath.ofproto
        if reason == ofproto.OFPPR_ADD:
            self.logger.info("port added %s", port_no)
        elif reason == ofproto.OFPPR_DELETE:
            self.logger.info("port deleted %s", port_no)
        elif reason == ofproto.OFPPR_MODIFY:
            self.logger.info("port modified %s", port_no)
        else:
            self.logger.info("Illegal port state %s %s", port_no, reason)
