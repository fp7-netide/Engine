################################################################################
# Backend for the Ryu Controller platform                                      #
# NetIDE FP7 Project: www.netide.eu, github.com/fp7-netide                     #
# author: Roberto Doriguzzi Corin (roberto.doriguzzi@create-net.org)           #
################################################################################
# Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica           #
# Investigacion Y Desarrollo SA (TID), Fujitsu Technology Solutions GmbH (FTS),#
# Thales Communications & Security SAS (THALES), Fundacion Imdea Networks      #
# (IMDEA), Universitaet Paderborn (UPB), Intel Research & Innovation Ireland   #
# Ltd (IRIIL), Fraunhofer-Institut fur Produktionstechnologie (IPT), Telcaria  #
# Ideas SL (TELCA)                                                             #
#                                                                              #
# All rights reserved. This program and the accompanying materials             #
# are made available under the terms of the Eclipse Public License v1.0        #
# which accompanies this distribution, and is available at                     #
# http://www.eclipse.org/legal/epl-v10.html                                    #
################################################################################

import os
import logging
import struct
import sys
import socket
import random
import binascii
import time
import ryu
import inspect
from eventlet.green import zmq
from eventlet.green import select
from eventlet.green import threading
from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller import dpset
from ryu.controller import controller
from ryu.ofproto import ofproto_parser
from ryu.ofproto import ofproto_common
from ryu.ofproto import ofproto_v1_0, ofproto_v1_0_parser
from ryu.ofproto import ofproto_v1_2, ofproto_v1_2_parser
from ryu.ofproto import ofproto_v1_3, ofproto_v1_3_parser
from ryu.ofproto import ofproto_v1_4, ofproto_v1_4_parser
from ryu.ofproto import ofproto_v1_5, ofproto_v1_5_parser
from ryu.controller.handler import HANDSHAKE_DISPATCHER, CONFIG_DISPATCHER, MAIN_DISPATCHER
from ryu.netide.netip import *


NETIDE_CORE_PORT = 5555
HEARTBEAT_TIMEOUT = 5 #5 seconds

logger = logging.getLogger('ryu-backend')
logger.setLevel(logging.DEBUG)


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
        self.netide_xid = 0
        self.ofp_brick = ryu.base.app_manager.lookup_service_brick('ofp_event')
        self.set_state(HANDSHAKE_DISPATCHER)
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
        msg_to_send = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', self.netide_xid, module_id, msg.datapath.id, str(msg.buf))
        logger.debug("Sent Message header: %s", NetIDEOps.netIDE_decode_header(msg_to_send))
        logger.debug("Sent Message body: %s",':'.join(x.encode('hex') for x in msg_to_send[NetIDEOps.NetIDE_Header_Size:]))
        self.channel.socket.send(msg_to_send)



    #Handles the events and sends them to the listening RYU Applications
    def handle_event(self, header, msg):
        #required_len = self.ofp.OFP_HEADER_SIZE
        ret = bytearray(msg)
        (version, msg_type, msg_len, xid) = ofproto_parser.header(ret)
        self.netide_xid = header[NetIDEOps.NetIDE_header['XID']]
        msg = ofproto_parser.msg(self, version, msg_type, msg_len, xid, ret)

        if msg and self.netide_xid is not 0 :
            ev = ofp_event.ofp_msg_to_ev(msg)
            event_observers = self.ofp_brick.get_observers(ev,self.state)
            module_id = header[NetIDEOps.NetIDE_header['MOD_ID']]
            for key, value in self.channel.running_modules.iteritems():
                if value == module_id and key in event_observers:
                    module_brick = ryu.base.app_manager.lookup_service_brick(key)
                    module_brick_handlers = module_brick.get_handlers(ev)
                    for handler in module_brick_handlers:
                        print "START calling the handler from backend", ev
                        handler(ev)
                        print "END calling the handler from backend"
                    break

            # Sending the FENCE message to the Core
            msg_to_send = NetIDEOps.netIDE_encode('NETIDE_FENCE', self.netide_xid, module_id, 0, "")
            self.channel.socket.send(msg_to_send)

            dispatchers = lambda x: x.callers[ev.__class__].dispatchers
            handlers = [handler for handler in
                        self.ofp_brick.get_handlers(ev) if
                        self.state in dispatchers(handler)]

            for handler in handlers:
                handler(ev)

            # Resetting netide_xid to zero
            self.netide_xid = 0

# Heartbeat thread
class HeatbeatThread(threading.Thread):
    def __init__(self,channel):
        threading.Thread.__init__(self)
        self.channel = channel


    def run(self):
        while True:
            time.sleep(HEARTBEAT_TIMEOUT)
            if self.channel.backend.backend_id > 0: #the backend has already received an identifier from the core
                hb_message = NetIDEOps.netIDE_encode('NETIDE_HEARTBEAT', None, self.channel.backend.backend_id, None, None)
                self.channel.socket.send(hb_message)

                #sending again hello messages if we haven't got any reply yet
                if self.channel.backend_info['connected'] == False:
                    logger.debug("Waiting for the Handshake to be completed...")
                    self.channel.send_handshake(self.channel.backend)


# Connection with the core
class CoreConnection(threading.Thread):
    def __init__(self, backend, host, port):
        threading.Thread.__init__(self)
        self.host = host
        self.port = port
        self.backend_info = {'negotiated_protocols':{}, 'datapaths':{}, 'connected' : False}
        self.running_modules = {}
        self.of_datapath = None
        self.ofproto = None
        self.ofproto_parser = None
        self.backend = backend
        self.heartbeat_time = time.time()
        context = zmq.Context()
        self.socket = context.socket(zmq.DEALER)
        self.socket.setsockopt(zmq.IDENTITY, self.backend.backend_name)

    def run(self):
        logger.debug('Connecting to Core on %s:%s...', self.host, self.port)
        self.socket.connect("tcp://" +str(self.host) + ":" + str(self.port))

        # Announcing the client controller to the core
        if self.backend_announcement(self.backend) is False:
            print "No backend id received from the core!!! Exiting..."
            return

        # Announcing the modules to the core
        # TODO: it seems that the ofp_event is always the last module registered by Ryu: to be checked!!!
        while 'ofp_event' not in app_manager.SERVICE_BRICKS:
            time.sleep(2)
        if self.module_announcement(app_manager.SERVICE_BRICKS) is False:
            print "No module ids received from the core!!! Exiting..."
            return

        # Performing the initial handshake with the shim
        logger.debug( "Starting the handshake process...")
        self.send_handshake(self.backend)

        #self.socket.send(b"First Hello from " + self.id)
        while True:
            message = self.socket.recv_multipart()
            msg = self.get_multipart_message(message) #patch to support the java-core message format
            self.handle_read(msg)

        self.socket.close()
        context.term()

    def get_multipart_message(self,msg):
        for part in msg:
            if len(part) > 0:
                return part

    def backend_announcement(self, backend):
        ann_message = NetIDEOps.netIDE_encode('MODULE_ANNOUNCEMENT', None, None, None, backend.backend_name)
        self.socket.send(ann_message)
        ack = False
        while ack is False:
            ack_message = self.socket.recv_multipart()
            msg = self.get_multipart_message(ack_message)
            decoded_header = NetIDEOps.netIDE_decode_header(msg)
            if decoded_header is False:
                continue
            message_type = decoded_header[NetIDEOps.NetIDE_header['TYPE']]
            message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
            if message_length is 0:
                continue
            else:
                message_data = msg[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
            if message_type is NetIDEOps.NetIDE_type['MODULE_ACKNOWLEDGE'] and message_data == backend.backend_name:
                logger.debug( "Received ack from Core: %s" , ack_message)
                backend_id = decoded_header[NetIDEOps.NetIDE_header['MOD_ID']]
                backend.backend_id = backend_id
                ack = True
        return True

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
                    msg = self.get_multipart_message(ack_message)
                    decoded_header = NetIDEOps.netIDE_decode_header(msg)
                    if decoded_header is False:
                        continue
                    message_type = decoded_header[NetIDEOps.NetIDE_header['TYPE']]
                    message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
                    if message_length is 0:
                        continue
                    else:
                        message_data = msg[NetIDEOps.NetIDE_Header_Size:]
                    if message_type is NetIDEOps.NetIDE_type['MODULE_ACKNOWLEDGE'] and message_data == module_name:
                        logger.debug( "Received ack from Core: %s" , ack_message)
                        module_id = decoded_header[NetIDEOps.NetIDE_header['MOD_ID']]
                        self.running_modules[module_name] = module_id
                        ack = True
        return True

    def send_handshake(self,backend):
        proto_data = NetIDEOps.netIDE_encode_handshake(self.backend.supported_protocols)
        message = NetIDEOps.netIDE_encode('NETIDE_HELLO', None, backend.backend_id, None, proto_data)
        self.socket.send(message)

    def handle_read(self,msg):
        decoded_header = NetIDEOps.netIDE_decode_header(msg)
        logger.debug("Received Message header: %s", decoded_header)
        message_type = NetIDEOps.key_by_value(NetIDEOps.NetIDE_type,decoded_header[NetIDEOps.NetIDE_header['TYPE']])
        logger.debug("Message type: %s", message_type)
        message_xid = decoded_header[NetIDEOps.NetIDE_header['XID']]
        logger.debug("Message xid: %s", message_xid)
        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
        message_data = msg[NetIDEOps.NetIDE_Header_Size:]
        logger.debug("Received Message body: %s",':'.join(x.encode('hex') for x in message_data))

        # control messages are processed only if the handshake has been successfully completeds
        if message_type is 'NETIDE_OPENFLOW' and self.backend_info['connected'] == True:
            if decoded_header[NetIDEOps.NetIDE_header['DPID']] not in self.backend_info['datapaths']:
                self.of_datapath = BackendDatapath(decoded_header[NetIDEOps.NetIDE_header['DPID']], self, self.ofproto, self.ofproto_parser)
                self.backend_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]] = self.of_datapath
                self.of_datapath.of_hello_handler(decoded_header)
            else:
                self.of_datapath = self.backend_info['datapaths'][decoded_header[NetIDEOps.NetIDE_header['DPID']]]

            self.of_datapath.handle_event(decoded_header, message_data)

        elif message_type is 'NETIDE_HELLO':
            if decoded_header is False:
                return False
            if message_length is 0:
                return False

            if self.backend_info['connected'] is False:
                count = 0
                openflow_version = 0
                supported_protocol_count = 0
                negotiated_protocols = self.backend_info['negotiated_protocols']
                message_data = NetIDEOps.netIDE_decode_handshake(message_data, message_length)
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
                if supported_protocol_count > 0:
                    self.backend_info['connected'] = True
                    logger.debug("Handshake completed!!!")

        elif message_type is 'NETIDE_ERROR':
            return False


class RyuBackend(app_manager.RyuApp):

    def __init__(self, *args, **kwargs):
        super(RyuBackend, self).__init__(*args, **kwargs)

        #Various Variables that can be edited
        __CORE_IP__ = '127.0.0.1'
        __CORE_PORT__ = NETIDE_CORE_PORT

        #Ryu Controller listen IP and port
        __CONTROLLER_IP__ = '127.0.0.1'
        __CONTROLLER_PORT__ = 7733

        self.backend_name = b"backend-ryu-" + str(os.getpid())
        self.backend_id = 0
        self.supported_protocols = {}
        #TODO: to implement an automatic mechanism that discovers the OF version used by the apps
        self.supported_protocols[OPENFLOW_PROTO] = [OPENFLOW_10,OPENFLOW_12,OPENFLOW_13]
        self.supported_protocols[NETCONF_PROTO] = []
        logger.debug( "Backend supported protocols: %s" , self.supported_protocols)
        logger.debug('RYU Backend initiated: %s', self.backend_name)

        #Start the zeromq loop to listen to incoming messages
        self.CoreConnection = CoreConnection(self, __CORE_IP__,__CORE_PORT__)
        self.CoreConnection.setDaemon(True)
        self.CoreConnection.start()

        #Start the heartbeat loop
        self.hb_thread = HeatbeatThread(self.CoreConnection)
        self.hb_thread.setDaemon(True)
        self.hb_thread.start()



