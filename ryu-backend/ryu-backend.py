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

import os
import logging
import struct
import threading
import sys
import random
import binascii
import time
import ryu
import socket
import inspect
from eventlet.green import zmq
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


NETIDE_CORE_PORT = 51515


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

    def of_hello_handler(self, netide_header):
        version = self.ofproto.OFP_VERSION
        msg_type = self.ofproto.OFPT_HELLO
        msg_len = self.ofproto.OFP_HEADER_SIZE
        data = '\x00' * 4
        xid = self.get_xid()

        fmt = self.ofproto.OFP_HEADER_PACK_STR
        buf = struct.pack(fmt, version, msg_type, msg_len, xid) + data
        #print hello.type
        self.handle_event(netide_header, buf)
        self.set_state(CONFIG_DISPATCHER)

    #Sends the reply from the switch back to the shim
    def send_msg(self, msg):
        assert isinstance(msg, self.ofproto_parser.MsgBase)
        module_id = None
        frame = inspect.currentframe()
        try:
            calling_object = str(frame.f_back.f_locals['self'])
            for key, value in self.channel.running_modules.iteritems():
                if calling_object.find(key) >= 0:
                    module_id = value
                    break
        finally:
            del frame

        if msg.xid is None:
            self.set_xid(msg)
        msg.serialize()
        msg_to_send = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', None, module_id, msg.datapath.id, str(msg.buf))
        self.channel.socket.send(msg_to_send)
        # LOG.debug('send_msg %s', msg)
        #print msg.buf
        #self.send(msg.buf)



    #Handles the events and sends them to the listening RYU Applications
    def handle_event(self, header, msg):
        #required_len = self.ofp.OFP_HEADER_SIZE
        ret = bytearray(msg)

        (version, msg_type, msg_len, xid) = ofproto_parser.header(ret)
        msg = ofproto_parser.msg(self, version, msg_type, msg_len, xid, ret)
        if msg:
            ev = ofp_event.ofp_msg_to_ev(msg)
            module_id = header[NetIDEOps.NetIDE_header['MOD_ID']]
            if module_id  == 0: # to all observers
                self.ofp_brick.send_event_to_observers(ev, self.state)
            else: # to a specific observer
                for key, value in self.channel.running_modules.iteritems():
                    if value == module_id:
                        module_name = key
                        break
                self.ofp_brick.send_event(module_name,ev, self.state)

            dispatchers = lambda x: x.callers[ev.__class__].dispatchers
            handlers = [handler for handler in
                        self.ofp_brick.get_handlers(ev) if
                        self.state in dispatchers(handler)]

            for handler in handlers:
                handler(ev)

# Connection with the core
class CoreConnection(threading.Thread):
    def __init__(self, backend, host, port):
        threading.Thread.__init__(self)
        self.host = host
        self.port = port
        self.client_info = {'negotiated_protocols':{}, 'datapaths':{}}
        self.running_modules = {}
        self.of_datapath = None
        self.ofproto = None
        self.ofproto_parser = None
        self.backend = backend
        self.id = self.backend.backend_id

    def run(self):
        context = zmq.Context()
        self.socket = context.socket(zmq.DEALER)
        self.socket.setsockopt(zmq.IDENTITY, self.id)
        print('Connecting to Core on %s:%s...' % (self.host,self.port))
        self.socket.connect("tcp://" +str(self.host) + ":" + str(self.port))

        # Performing the initial handshake with the shim
        # TODO: I comment the handshake for now since the Core only supports OF1.0. If we want to keep it, it should be moved after the module announcement
        #if self.handle_handshake() is False:
        #    print "Handshake error!!! Exiting..."
        #    return
        time.sleep(2) #TODO: we need to wait until the app_manager has detected all the running apps. Replace the sleep with something more elegant.
        app_manager.AppManager.report_bricks()
        if self.module_announcement(app_manager.SERVICE_BRICKS) is False:
            print "No module ids received from the core!!! Exiting..."
            return

        #self.socket.send(b"First Hello from " + self.id)
        while True:
            #message = self.socket.recv()
            message = self.socket.recv_multipart()
            print "Received message from Core:" ,':'.join(x.encode('hex') for x in message[0])
            self.handle_read(message[0])

        self.socket.close()
        context.term()

    def module_announcement(self, bricks):
        for key,value in bricks.iteritems():
            module_name = key
            if key is not "ofp_event" and key is not self.backend.name: #we take only the control applications
                xid = random.randint(0, 1000000)
                ann_message = NetIDEOps.netIDE_encode('MODULE_ANNOUNCEMENT', xid, None, None, module_name)
                self.socket.send(ann_message)
                ack = False
                while ack is False:
                    ack_message = self.socket.recv_multipart()
                    msg = ack_message[0]
                    decoded_header = NetIDEOps.netIDE_decode_header(msg)
                    message_type = decoded_header[NetIDEOps.NetIDE_header['TYPE']]
                    if message_type is NetIDEOps.NetIDE_type['MODULE_ACKNOWLEDGE'] and decoded_header[NetIDEOps.NetIDE_header['XID']] == xid:
                        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
                        message_data = msg[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
                        self.running_modules[module_name] = int(message_data)
                        ack = True



    def handle_handshake(self):
        proto_data = NetIDEOps.netIDE_encode_handshake(self.backend.supported_protocols)
        message = NetIDEOps.netIDE_encode('NETIDE_HELLO', None, None, None, proto_data)
        self.socket.send(message)

        message = self.socket.recv_multipart()
        msg = message[0]
        decoded_header = NetIDEOps.netIDE_decode_header(msg)
        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
        message_type = decoded_header[NetIDEOps.NetIDE_header['TYPE']]

        if message_type is NetIDEOps.NetIDE_type['NETIDE_HELLO']:
            message_data = NetIDEOps.netIDE_decode_handshake(msg[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length],message_length)
            if 'connected' not in self.client_info:
                count = 0
                openflow_version = 0
                supported_protocol_count = 0
                negotiated_protocols = self.client_info['negotiated_protocols']
                while count < message_length:
                    protocol = message_data[count]
                    version = message_data[count + 1]
                    count += 2

                    if version in self.backend.supported_protocols[protocol]:
                        supported_protocol_count += 1
                        if protocol in negotiated_protocols:
                            negotiated_protocols[protocol].append(version)
                        else:
                            negotiated_protocols.update({protocol:[version]})
                    if protocol == NetIDEOps.NetIDE_type['NETIDE_OPENFLOW'] and version > openflow_version:
                        openflow_version = version
                        if openflow_version == 0x01:
                            self.ofproto = ofproto_v1_0
                            self.ofproto_parser = ofproto_v1_0_parser
                        if openflow_version == 0x03:
                            self.ofproto = ofproto_v1_2
                            self.ofproto_parser = ofproto_v1_2_parser
                        if openflow_version == 0x04:
                            self.ofproto = ofproto_v1_3
                            self.ofproto_parser = ofproto_v1_3_parser
                        if openflow_version == 0x05:
                            self.ofproto = ofproto_v1_4
                            self.ofproto_parser = ofproto_v1_4_parser
                        if openflow_version == 0x06:
                            self.ofproto = ofproto_v1_5
                            self.ofproto_parser = ofproto_v1_5_parser
                if supported_protocol_count == 0:
                    return False
                else:
                    self.client_info['connected'] = True
            return True
        else:
            return False

    def handle_read(self,msg):
        decoded_header = NetIDEOps.netIDE_decode_header(msg)
        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
        message_data = msg[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
        #print (':'.join(x.encode('hex') for x in message_data))
        message_type = decoded_header[NetIDEOps.NetIDE_header['TYPE']]
        if message_type is NetIDEOps.NetIDE_type['NETIDE_OPENFLOW']:
            if decoded_header[NetIDEOps.NetIDE_header['DPID']] not in self.client_info['datapaths']:
                self.of_datapath = BackendDatapath(decoded_header[NetIDEOps.NetIDE_header['DPID']], self, self.ofproto, self.ofproto_parser)
                self.client_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]] = self.of_datapath
                self.of_datapath.of_hello_handler(decoded_header)
            else:
                self.of_datapath = self.client_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]]
                #print self.client_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]]

            self.of_datapath.handle_event(decoded_header, message_data)



class RYUClient(app_manager.RyuApp):

    def __init__(self, *args, **kwargs):
        super(RYUClient, self).__init__(*args, **kwargs)

        #Various Variables that can be edited
        __CORE_IP__ = '127.0.0.1'
        __CORE_PORT__ = NETIDE_CORE_PORT

        #Ryu Controller listen IP and port
        __CONTROLLER_IP__ = '127.0.0.1'
        __CONTROLLER_PORT__ = 7733

        self.backend_id = b"backend-ryu-" + str(os.getpid())
        self.supported_protocols = {}
        self.supported_protocols[OPENFLOW_PROTO] = [OPENFLOW_10,OPENFLOW_12,OPENFLOW_13]
        self.supported_protocols[NETCONF_PROTO] = []
        print "backend supported protocols: ", self.supported_protocols

        print('RYU Client initiated')

        #Start the zeromq loop to listen to incoming messages
        self.CoreConnection = CoreConnection(self, __CORE_IP__,__CORE_PORT__)
        self.CoreConnection.setDaemon(True)
        self.CoreConnection.start()



