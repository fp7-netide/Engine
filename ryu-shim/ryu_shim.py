##########################################################################
# Ryu backend for the shim client                                              #
# NetIDE FP7 Project: www.netide.eu, github.com/fp7-netide                     #
# author: Roberto Doriguzzi Corin (roberto.doriguzzi@create-net.org)           #
##########################################################################
# Eclipse Public License - v 1.0                                               #
#                                                                              #
# THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC  #
# LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM  #
# CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.                        #
##########################################################################

import logging
import struct
import threading
import sys
import random
import binascii
import zmq
import time
from ryu.base import app_manager
from ryu.exception import RyuException
from ryu.controller import mac_to_port
from ryu.controller import ofp_event
from ryu.controller import dpset
from ryu.controller.handler import MAIN_DISPATCHER, set_ev_handler
from ryu.controller.handler import HANDSHAKE_DISPATCHER
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_0, ether, ofproto_v1_0_parser, nx_match
from ryu.lib.mac import haddr_to_bin, DONTCARE_STR
from ryu.lib.dpid import dpid_to_str, str_to_dpid
from ryu.lib.ip import ipv4_to_bin, ipv4_to_str
from ryu.lib.packet import packet, ethernet, lldp
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4
from netide.comm import *

def inport_value_hack(outport):
    if outport > 1:
        return 1
    else:
        return 2
    
class OF10Match(object):
    def __init__(self):
        self.wildcards=None
        self.in_port = None
        self.dl_src = None
        self.dl_dst = None
        self.dl_type = None
        self.dl_vlan = None
        self.dl_vlan_pcp = None
        self.nw_tos = None
        self.nw_proto = None
        self.nw_src = None
        self.nw_dst = None
        self.tp_dst = None
        self.tp_src = None
    
    def match_tuple(self):
        """return a tuple which can be used as *args for
        ofproto_v1_0_parser.OFPMatch.__init__().
        see Datapath.send_flow_mod.
        """
        return (self.wildcards, self.in_port, self.dl_src,
                self.dl_dst, self.dl_vlan, self.dl_vlan_pcp,
                self.dl_type, self.nw_tos,
                self.nw_proto, self.nw_src, self.nw_dst,
                self.tp_src, self.tp_dst)


class BackendChannel(asynchat.async_chat):
    """Sends messages to the server and receives responses.
    """
    def __init__(self, host, port, of_client):
        self.of_client = of_client
        self.received_data = []
        asynchat.async_chat.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.connect((host, port))
        self.ac_in_buffer_size = 4096 * 3
        self.ac_out_buffer_size = 4096 * 3
        self.set_terminator(TERM_CHAR)
        return

    def handle_connect(self):
        print "Connected to pyretic frontend."
        
    def collect_incoming_data(self, data):
        """Read an incoming message from the client and put it into our outgoing queue."""
        #print "ofclient collect incoming data"
        with self.of_client.channel_lock:
            self.received_data.append(data)

    def dict2OF(self,d):
        def convert(h,val):
            if h in ['srcmac','dstmac']:
                return val #packetaddr.EthAddr(val)
            elif h in ['srcip','dstip']:
                try:
                    return val #packetaddr.IPAddr(val)
                except:
                    return val
            elif h in ['vlan_id','vlan_pcp'] and val == 'None':
                return None
            else:
                return val
        return { h : convert(h,val) for (h, val) in d.items()}
    def found_terminator(self):
        """The end of a command or message has been seen."""
        with self.of_client.channel_lock:
            msg = deserialize(self.received_data)

        # USE DESERIALIZED MSG
        if msg[0] == 'inject_discovery_packet':
            switch = msg[1]
            port = msg[2]
            self.of_client.inject_discovery_packet(switch,port)
        elif msg[0] == 'packet':
            packet = self.dict2OF(msg[1])
            self.of_client.send_to_switch(packet)
        elif msg[0] == 'install':
            pred = self.dict2OF(msg[1])
            priority = int(msg[2])
            actions = map(self.dict2OF,msg[3])
            self.of_client.install_flow(pred,priority,actions)
        elif msg[0] == 'delete':
            pred = self.dict2OF(msg[1])
            priority = int(msg[2])
            self.of_client.delete_flow(pred,priority)
        elif msg[0] == 'clear':
            switch = int(msg[1])
            self.of_client.clear(switch)
        elif msg[0] == 'barrier':
            switch = msg[1]
            self.of_client.barrier(switch)
        elif msg[0] == 'flow_stats_request':
            switch = msg[1]
            self.of_client.flow_stats_request(switch)
        else:
            print "ERROR: Unknown msg from frontend %s" % msg

# Connects to the Core
class CoreConnection(threading.Thread):
    def __init__(self, id, host, port):
    	threading.Thread.__init__(self)
        self.id = id
        self.host = host
        self.port = port

    def run(self):
        print('Connecting to Core on %s:%s...' % (self.host,self.port))
        context = zmq.Context()
        self.socket = context.socket(zmq.DEALER)
        self.socket.setsockopt(zmq.IDENTITY, self.id)
        self.socket.connect("tcp://" +str(self.host) + ":" + str(self.port))
        print('Connected to Core.')
        #self.socket.send(b"First Hello from " + self.id)
        while True:
            message = self.socket.recv_multipart()
            # TODO: handle message
            print("Received message from Core: %s" % message[0])


class RYUClient(app_manager.RyuApp):
    def __init__(self, *args, **kwargs):
        super(RYUClient, self).__init__(*args, **kwargs)

        # Various Variables that can be edited
        __LISTEN_IP__ = 'localhost'
        __LISTEN_PORT__ = 5555#RYU_BACKEND_PORT #Default Port 41414

        # Internal variables
        self.switches = {}
        self.ofp_version = None

        # Default listen IP and port
        address = (__LISTEN_IP__, __LISTEN_PORT__)

        # Start the connection to the core
        print('RYU Shim initiated')

        # self.backend_server = BackendServer(self,address)
        self.CoreConnection = CoreConnection("Ryu Shim", __LISTEN_IP__,__LISTEN_PORT__)
        self.CoreConnection.start()
        self.register_all_events()

    # Explicitly sends a feature request to all the switches
    # OF Only? To check for other protocols!
    def send_features_request(self):
        for datapath_id, datapath in self.switches.iteritems():
            ofp_parser = datapath.ofproto_parser
            req = ofp_parser.OFPFeaturesRequest(datapath)
            datapath.send_msg(req)

    # Forwards all messags recieved from the switches to the clients
    def _event_loop(self):
        # First hello message from switch to get the version for negotiation
        ev, state = self.events.get()
        print ev.msg.version
        self.ofp_version = ev.msg.version
        NetIDEOps.NetIDE_supported_protocols[0x11] = {self.ofp_version}
        # print ev.msg
        while self.is_active:
            ev, state = self.events.get()
            if ev == self._event_stop:
                continue
            self.send_to_clients(ev)

    # Function registers all events to be observed
    def register_all_events(self):
        for event in ofp_event._OFP_MSG_EVENTS:
            self.observe_event(ofp_event._OFP_MSG_EVENTS[event])


    # Register a single event to keep the app running
    @set_ev_cls(ofp_event.EventOFPHello)
    def _handle_Hello(self, ev):
        pass

    # Sends the message to the connected NetIDE clients
    def send_to_clients(self, ev):
        msg = ev.msg
        self.datapath = ev.msg.datapath
        # Add all the switches connected datapath.id and the connection information to the local variable
        if self.datapath.id not in self.switches:
            self.switches[self.datapath.id] = self.datapath

        # Encapsulate the feature request and send to connected client
        msg_to_send = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', None, self.datapath.id, str(msg.buf))
        # Forward the message to all the connected NetIDE clients
        # for client in self.backend_server.clients:
        #    client.send(msg_to_send)
        self.CoreConnection.socket.send(msg_to_send)


class NetIDEOps:

    # Define the NetIDE Version
    NetIDE_version = 0x01
    # Define the NetIDE message types and codes
    NetIDE_type = {
        'NETIDE_HELLO'      : 0x01,
        'NETIDE_ERROR'      : 0x40,
        'NETIDE_OPENFLOW'   : 0x11,
        'NETIDE_NETCONF'    : 0x12,
        'NETIDE_OPFLEX'     : 0x13
    }

    # Define the supported switch control protocols we support and versios
    # Should be determined by underlying network/switches??
    # Protocol:
    # 0x10 = OpenFlow: versions - 0x01 = 1.0; 0x02 = 1.1; 0x03 = 1.2; 0x04 = 1.3; 0x05 = 1.4
    # 0x11 = NetConf: versions - 0x01 = RFC6241 of NetConf
    # 0x12 = OpFlex: versions - 0x00 = Version in development
    NetIDE_supported_protocols = {
        0x11    : {0x01, 0x02, 0x03, 0x04, 0x5},
        0x12    : {0x01},
        0x13    : {0x00}
    }

 # Encode a message in the NetIDE protocol format
    @staticmethod
    def netIDE_encode(type, xid, datapath_id, msg):
        length = len(msg)
        type_code = NetIDEOps.NetIDE_type[type]
        # if no transaction id is given, generate a random one.
        if xid is None:
            xid = random.getrandbits(32)
        if datapath_id is None:
            datapath_id = 0
        values = (NetIDEOps.NetIDE_version, type_code, length, xid, datapath_id, msg)
        packer = struct.Struct('!BBHIQ'+str(length)+'s')
        packed_msg = packer.pack(*values)
        return packed_msg

    # Decode NetIDE header of a message (first 16 Bytes of the read message
    @staticmethod
    def netIDE_decode_header(raw_data):
        unpacker = struct.Struct('!BBHIQ')
        return unpacker.unpack(raw_data)

    # Decode NetIDE messages received in binary format. Iput: Raw data and length of the encapsulated message
    # Length can be retrieve by decoding the header first
    # NEEDS TO BE CHANGED MAYBE?
    @staticmethod
    def netIDE_decode(raw_data, length):
        unpacker = struct.Struct('!BBHIQ'+str(length)+'s')
        return unpacker.unpack(raw_data)

    # Encode the hello handshake message
    @staticmethod
    def netIDE_encode_handshake(protocols):

        # Get count for number of supported protocols and versions
        count = 0
        values = []
        for protocol in protocols.items():
            for version in protocol[1]:
                count += 1
                values.append(protocol[0])
                values.append(version)

        packer = struct.Struct('!'+str(count*2)+'B')
        return packer.pack(*values)

    # Decode the hello handshake message and return tuple
    @staticmethod
    def netIDE_decode_handshake(raw_data, length):
        packer = struct.Struct('!'+str(length)+'B')
        unpacked = packer.unpack(raw_data)
        return unpacked

    # Return the key name from a value in a dictionary
    @staticmethod
    def key_by_value(dictionary, value):
        for key, val in dictionary:
            if value == val:
                return key
