################################################################################
# Ryu backend for the shim client                                              #
# NetIDE FP7 Project: www.netide.eu, github.com/fp7-netide                     #
# author: Roberto Doriguzzi Corin (roberto.doriguzzi@create-net.org)           #
################################################################################
# Eclipse Public License - v 1.0                                               #
#                                                                              #
# THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC  #
# LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM  #
# CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.                        #
################################################################################

import logging
import struct
import threading
import sys
import random
import binascii
import time
import ryu
from ryu.base import app_manager
from ryu.exception import RyuException
from ryu.controller import mac_to_port
from ryu.controller import ofp_event
from ryu.controller import dpset
from ryu.controller import controller
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import HANDSHAKE_DISPATCHER
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.lib import hub
from ryu.ofproto import ofproto_v1_0, ether, ofproto_v1_0_parser, nx_match
from ryu.lib.mac import haddr_to_bin, DONTCARE_STR
from ryu.lib.dpid import dpid_to_str, str_to_dpid
from ryu.lib.ip import ipv4_to_bin, ipv4_to_str
from ryu.lib.packet import packet, ethernet, lldp
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4
from ryu.ofproto import ofproto_protocol
from ryu.ofproto import ofproto_parser
from ryu.ofproto import ofproto_v1_0, ofproto_v1_0_parser
from ryu.ofproto import ofproto_v1_2, ofproto_v1_2_parser
from ryu.ofproto import ofproto_v1_3, ofproto_v1_3_parser
from ryu.ofproto import ofproto_v1_4, ofproto_v1_4_parser
from ryu.ofproto import ofproto_v1_5, ofproto_v1_5_parser
from ryu.controller.handler import HANDSHAKE_DISPATCHER, CONFIG_DISPATCHER, MAIN_DISPATCHER
from ryu.netide.comm import *




#
class BackendChannel(asyncore.dispatcher):
    """Handles the data channel to the server
    """
    def __init__(self, host, port, controller):
        self.channel_lock = threading.Lock()
        self.client_info = {'negotiated_protocols':{}, 'datapaths':{}}
        asyncore.dispatcher.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.connect((host, port))
        self.of_datapath = None
        self.ofproto = None
        self.ofproto_parser = None
        self.controller = controller
        self.controller_channel = ControllerChannel('127.0.0.1', 7733)
        return

    def handle_connect(self):
        #Initiate handshake with the server
        proto_data = NetIDEOps.netIDE_encode_handshake(NetIDEOps.NetIDE_supported_protocols)
        msg = NetIDEOps.netIDE_encode('NETIDE_HELLO', None, 0, proto_data)
        print msg.encode("hex")
        self.send(msg)



    def handle_read(self):
        header = self.recv(16)
        decoded_header = NetIDEOps.netIDE_decode_header(header)
        message_length = decoded_header[2]
        message_data = self.recv(message_length)

        message_data = NetIDEOps.netIDE_decode_handshake(message_data, message_length)
        #If new client is connecting, add the protocols that were negotiated with the server
        if 'connected' not in self.client_info:
            with self.channel_lock:
                count = 0
                while count < message_length:
                    protocol = message_data[count]
                    version = message_data[count + 1]

                    if protocol not in self.client_info['negotiated_protocols']:
                        self.client_info['negotiated_protocols'][protocol] = {version}
                    else:
                        self.client_info['negotiated_protocols'][protocol].add(version)

                    count += 2

                self.client_info['connected'] = True

                #TO DO: For Multiple versions that the switch might support!
                #Create OpenFlow backend that communicates with the controller (simulates the switch)
                #If the switch supported OpenFlow
                if 0x11 in self.client_info['negotiated_protocols']:
                    ofp_version = list(self.client_info['negotiated_protocols'][0x11])[0]
                    if ofp_version == 0x01:
                        self.ofproto = ofproto_v1_0
                        self.ofproto_parser = ofproto_v1_0_parser
                    elif ofp_version == 0x03:
                        self.ofproto = ofproto_v1_3
                        self.ofproto_parser = ofproto_v1_3_parser
                    elif ofp_version == 0x04:
                        self.ofproto = ofproto_v1_4
                        self.ofproto_parser = ofproto_v1_4_parser
                    else:
                        print "OF Protocol not supported"
                        self.close()

                self.of_datapath = BackendDatapath(decoded_header[4], self, self.ofproto, self.ofproto_parser)
                self.client_info['datapaths'][decoded_header[4]] = self.of_datapath
                self.of_datapath.of_hello_handler()

        #If client has been previously connected and handshake has been made
        #Make sure to select the correct datapath to send the message to!
        else:
            if decoded_header[4] not in self.client_info['datapaths']:
                self.of_datapath = BackendDatapath(decoded_header[4], self, self.ofproto, self.ofproto_parser)
                self.client_info['datapaths'][decoded_header[4]] = self.of_datapath
                self.of_datapath.of_hello_handler()
            else:
                self.of_datapath = self.client_info['datapaths'][decoded_header[4]]
                #print self.client_info['datapaths'][decoded_header[4]]

            with self.channel_lock:
                message_type = NetIDEOps.key_by_value(NetIDEOps.NetIDE_type, decoded_header[1])
                if message_type == 'NETIDE_OPENFLOW':
                    #print decoded_header
                    self.of_datapath.handle_event(message_data)


#Forwards the decapsulated messages to the controller and handles gets the response.
class ControllerChannel(asyncore.dispatcher):
    def __init__(self, host, port):
        asyncore.dispatcher.__init__(self)
        self.host = host
        self.port = port

    def switch_connet(self):
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.connect((self.host, self.port))

    def send_data(self, msg):
        self.send(msg)

    def receive_data(self):
        return self.recv(4096)




class BackendDatapath(controller.Datapath):
    def __init__(self, id, channel, ofp, ofpp):
        dummy_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        dummy_address = ('127.0.0.1', 0)
        super(BackendDatapath, self).__init__(dummy_socket, dummy_address)

        self.channel = channel
        self.ofp = ofp
        self.ofpp = ofpp
        self.id = id
        self.xid = 0
        self.ofp_brick = ryu.base.app_manager.lookup_service_brick('ofp_event')
        self.set_state(HANDSHAKE_DISPATCHER)
        self.flow_format = ofproto_v1_0.NXFF_OPENFLOW10
        self.is_active = True

    def handle_write(self):
        pass

    def get_xid(self):
        self.xid += 1
        self.xid &= self.ofproto.MAX_XID
        return self.xid


    def set_state(self, state):
        self.state = state
        ev = ofp_event.EventOFPStateChange(self)
        ev.state = state
        self.ofp_brick.send_event_to_observers(ev, state)

    def of_hello_handler(self):
        version = self.ofproto.OFP_VERSION
        msg_type = self.ofproto.OFPT_HELLO
        msg_len = self.ofproto.OFP_HEADER_SIZE
        data = '\x00' * 4
        xid = self.get_xid()

        fmt = self.ofproto.OFP_HEADER_PACK_STR
        buf = struct.pack(fmt, version, msg_type, msg_len, xid) + data
        #print hello.type
        self.handle_event(buf)
        self.set_state(CONFIG_DISPATCHER)

    #Sends the reply from the switch back to the shim
    def send_msg(self, msg):
        assert isinstance(msg, self.ofproto_parser.MsgBase)
        if msg.xid is None:
            self.set_xid(msg)
        msg.serialize()
        msg_to_send = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', None, msg.datapath.id, str(msg.buf))
        self.channel.send(msg_to_send)
        # LOG.debug('send_msg %s', msg)
        #print msg.buf
        #self.send(msg.buf)



    #Handles the events and sends them to the listening RYU Applications
    def handle_event(self, msg):
        #required_len = self.ofp.OFP_HEADER_SIZE
        ret = bytearray(msg)
        (version, msg_type, msg_len, xid) = ofproto_parser.header(ret)
        msg = ofproto_parser.msg(self, version, msg_type, msg_len, xid, ret)
        if msg:
            ev = ofp_event.ofp_msg_to_ev(msg)
            self.ofp_brick.send_event_to_observers(ev, self.state)

            dispatchers = lambda x: x.callers[ev.__class__].dispatchers
            handlers = [handler for handler in
                        self.ofp_brick.get_handlers(ev) if
                        self.state in dispatchers(handler)]
            #print "handlers: ", self.ofp_brick.get_handlers(ev)
            for handler in handlers:
                handler(ev)
                #print "Datapath ", self.id, " current state : ", self.state, " message type: ", msg.msg_type

#Starts the asyncore client as a threaded process so the application can continue running
class asyncore_loop(threading.Thread):
        def run(self):
            asyncore.loop()



class RYUClient(app_manager.RyuApp):

    def __init__(self, *args, **kwargs):
        super(RYUClient, self).__init__(*args, **kwargs)

        #Various Variables that can be edited
        __SERVER_IP__ = '127.0.0.1'
        __SERVER_PORT__ = RYU_BACKEND_PORT #Default Port 41414

        #Ryu Controller listen IP and port
        __CONTROLLER_IP__ = '127.0.0.1'
        __CONTROLLER_PORT__ = 7733

        #Start the asyncore loop (Server) to listen to incomming connections
        print('RYU Client initiated')
        self.backend_channel = BackendChannel(__SERVER_IP__, __SERVER_PORT__, self)
        self.al = asyncore_loop()
        self.al.start()









class NetIDEOps:

    #Define the NetIDE Version
    NetIDE_version = 0x01
    #Define the NetIDE message types and codes
    NetIDE_type = {
        'NETIDE_HELLO'      : 0x01,
        'NETIDE_ERROR'      : 0x40,
        'NETIDE_OPENFLOW'   : 0x11,
        'NETIDE_NETCONF'    : 0x12,
        'NETIDE_OPFLEX'     : 0x13
    }

    #Define the supported switch control protocols we support and versios
    #Should be determined by underlying network/switches??
    #Protocol:
    #0x11 = OpenFlow: versions - 0x01 = 1.0; 0x02 = 1.1; 0x03 = 1.2; 0x04 = 1.3; 0x05 = 1.4
    #0x12 = NetConf: versions - 0x01 = RFC6241 of NetConf
    #0x13 = OpFlex: versions - 0x00 = Version in development
    NetIDE_supported_protocols = {
        0x11    : {0x01, 0x02, 0x03, 0x04, 0x5},
        0x12    : {0x01},
        0x13    : {0x00},
        0x14    : {0x01}
    }

 #Encode a message in the NetIDE protocol format
    @staticmethod
    def netIDE_encode(type, xid, datapath_id, msg):
        length = len(msg)
        type_code = NetIDEOps.NetIDE_type[type]
        #if no transaction id is given, generate a random one.
        if xid is None:
            xid = random.getrandbits(32)
        values = (NetIDEOps.NetIDE_version, type_code, length, xid, datapath_id, msg)
        packer = struct.Struct('!BBHIQ'+str(length)+'s')
        packed_msg = packer.pack(*values)
        return packed_msg

    #Decode NetIDE header of a message (first 16 Bytes of the read message
    @staticmethod
    def netIDE_decode_header(raw_data):
        unpacker = struct.Struct('!BBHIQ')
        return unpacker.unpack(raw_data)

    #Decode NetIDE messages received in binary format. Iput: Raw data and length of the encapsulated message
    #Length can be retrieve by decoding the header first
    #NEEDS TO BE CHANGED MAYBE?
    @staticmethod
    def netIDE_decode(raw_data, length):
        unpacker = struct.Struct('!BBHIQ'+str(length)+'s')
        return unpacker.unpack(raw_data)

    #Encode the hello handshake message
    @staticmethod
    def netIDE_encode_handshake(protocols):

        #Get count for number of supported protocols and versions
        count = 0
        values = []
        for protocol in protocols.items():
            for version in protocol[1]:
                count += 1
                values.append(protocol[0])
                values.append(version)

        packer = struct.Struct('!'+str(count*2)+'B')
        return packer.pack(*values)


    #Decode the hello handshake message and return tuple
    @staticmethod
    def netIDE_decode_handshake(raw_data, length):
        packer = struct.Struct('!'+str(length)+'B')
        unpacked = packer.unpack(raw_data)
        return unpacked

    #Return the key name from a value in a dictionary
    @staticmethod
    def key_by_value(dictionary, value):
        for key, val in dictionary.iteritems():
            if value == val:
                return key



