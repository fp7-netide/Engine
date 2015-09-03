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
import asyncore
import socket
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
from ryu.netide.netip import *



NETIDE_CORE_PORT = 41414

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
        msg = NetIDEOps.netIDE_encode('NETIDE_HELLO', None, None, None, proto_data)
        print msg.encode("hex")
        self.send(msg)



    def handle_read(self):
        header = self.recv(NetIDE_Header_Size)
        decoded_header = NetIDEOps.netIDE_decode_header(header)
        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
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

                self.of_datapath = BackendDatapath(decoded_header[NetIDEOps.NetIDE_header['DPID']], self, self.ofproto, self.ofproto_parser)
                self.client_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]] = self.of_datapath
                self.of_datapath.of_hello_handler()

        #If client has been previously connected and handshake has been made
        #Make sure to select the correct datapath to send the message to!
        else:
            if decoded_header[NetIDEOps.NetIDE_header['DPID']] not in self.client_info['datapaths']:
                self.of_datapath = BackendDatapath(decoded_header[NetIDEOps.NetIDE_header['DPID']], self, self.ofproto, self.ofproto_parser)
                self.client_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]] = self.of_datapath
                self.of_datapath.of_hello_handler()
            else:
                self.of_datapath = self.client_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]]
                #print self.client_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]]

            with self.channel_lock:
                message_type = NetIDEOps.key_by_value(NetIDEOps.NetIDE_type, decoded_header[NetIDEOps.NetIDE_header['TYPE']])
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
        msg_to_send = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', None, None, msg.datapath.id, str(msg.buf))
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



