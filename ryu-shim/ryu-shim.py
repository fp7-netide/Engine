################################################################################
# Ryu Shim Layer                                                               #
# NetIDE FP7 Project: www.netide.eu, github.com/fp7-netide                     #
# author: Roberto Doriguzzi Corin (roberto.doriguzzi@create-net.org)           #
################################################################################
# Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica 	       #
# Investigacion Y Desarrollo SA (TID), Fujitsu Technology Solutions GmbH (FTS),#
# Thales Communications & Security SAS (THALES), Fundacion Imdea Networks      #
# (IMDEA), Universitaet Paderborn (UPB), Intel Research & Innovation Ireland   #
# Ltd (IRIIL), Fraunhofer-Institut fur Produktionstechnologie (IPT), Telcaria  #
# Ideas SL (TELCA)							                                   #
# 									                                           #
# All rights reserved. This program and the accompanying materials	           #
# are made available under the terms of the Eclipse Public License v1.0	       #
# which accompanies this distribution, and is available at		               #
# http://www.eclipse.org/legal/epl-v10.html                      	           #
################################################################################

import logging
import struct
import threading
import sys
import random
import binascii
import time
import socket
from eventlet.green import zmq
from ryu.base import app_manager
from ryu.exception import RyuException
from ryu.controller import mac_to_port
from ryu.controller import ofp_event
from ryu.controller import dpset
from ryu.controller.handler import MAIN_DISPATCHER, set_ev_handler, set_ev_cls
from ryu.controller.handler import HANDSHAKE_DISPATCHER
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.controller.ofp_handler import OFPHandler
from ryu.ofproto import ofproto_v1_0, ether, ofproto_v1_0_parser, nx_match
from ryu.lib.mac import haddr_to_bin, DONTCARE_STR
from ryu.lib.dpid import dpid_to_str, str_to_dpid
from ryu.lib.ip import ipv4_to_bin, ipv4_to_str
from ryu.lib.packet import packet, ethernet, lldp
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4
from netip import *
#from ryu.netide.netip import *
#from time import sleep

NETIDE_CORE_PORT = 41414

# Connection with the core
class CoreConnection(threading.Thread):
    def __init__(self, controller, id, host, port):
        threading.Thread.__init__(self)
        self.id = id
        self.host = host
        self.port = port
        self.controller = controller
        #TODO: improve the management of multiple clients

    def run(self):
        context = zmq.Context()
        self.socket = context.socket(zmq.DEALER)
        self.socket.setsockopt(zmq.IDENTITY, self.id)
        print('Connecting to Core on %s:%s...' % (self.host,self.port))
        self.socket.connect("tcp://" +str(self.host) + ":" + str(self.port))

        print('Connected to Core.')
        #self.socket.send(b"First Hello from " + self.id)
        while True:
            message = self.socket.recv_multipart()
            print("Received message from Core: ")
            print (':'.join(x.encode('hex') for x in message[0]))
            self.handle_read(message[0])

        self.socket.close()
        context.term()

    def handle_read(self,msg):
        decoded_header = NetIDEOps.netIDE_decode_header(msg)
        print "header: ", decoded_header
        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
        message_data = msg[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
        print (':'.join(x.encode('hex') for x in message_data))

        if decoded_header[NetIDEOps.NetIDE_header['VERSION']] is not NetIDEOps.NetIDE_version:
            print ("Attempt to connect from unsupported client")
            return
        else:
            #If new client is connecting
            if decoded_header[NetIDEOps.NetIDE_header['TYPE']] is NetIDEOps.NetIDE_type['NETIDE_HELLO']:

                if message_length is 0:
                    print ("Client does not support any protocol")
                    return

                message_data = NetIDEOps.netIDE_decode_handshake(message_data, message_length)
                negotiated_protocols = {}
                #Find the common protocols that client and server support
                count = 0
                while count < message_length:
                    protocol = message_data[count]
                    version = message_data[count+1]
                    count += 2

                    if version in self.controller.supported_protocols[protocol]:
                        if protocol in negotiated_protocols:
                            negotiated_protocols[protocol].append(version)
                        else:
                            negotiated_protocols.update({protocol:[version]})

                #After protocols have been negotiated, send back message to client to notify for common protocols
                proto_data = NetIDEOps.netIDE_encode_handshake(negotiated_protocols)
                msg = NetIDEOps.netIDE_encode('NETIDE_HELLO', None, None, None, proto_data)
                self.socket.send(msg)

                #Resend request for features for the new client
                self.controller.send_features_request()

            else:
                if message_length is 0:
                    print ("Empty message!!!")
                    return

                if decoded_header[NetIDEOps.NetIDE_header['DPID']] is not 0:
                    self.datapath = self.controller.switches[int(decoded_header[NetIDEOps.NetIDE_header['DPID']])]
                    self.datapath.send(message_data)
                else:
                    self.datapath = None

    
    
class RYUShim(app_manager.RyuApp):
    def __init__(self, *args, **kwargs):
        self.__class__.name = "RYUShim"
        super(RYUShim, self).__init__(*args, **kwargs)

        # Various Variables that can be edited
        __CORE_IP__ = 'localhost'
        __CORE_PORT__ = NETIDE_CORE_PORT

        # Internal variables
        self.switches = {}
        self.supported_protocols = {}
        self.supported_protocols[OPENFLOW_PROTO] = []
        self.supported_protocols[NETCONF_PROTO] = []
        self.ofp_version = None

        # Start the connection to the core
        print('RYU Shim initiated')
        self.CoreConnection = CoreConnection(self, b"shim", __CORE_IP__,__CORE_PORT__)
        self.CoreConnection.setDaemon(True)
        self.CoreConnection.start()

    def start_thread(self):
        self.CoreConnection.start()

    #Explicitly sends a feature request to all the switches
    #OF Only? To check for other protocols!
    def send_features_request(self):
        for datapath_id, datapath in self.switches.iteritems():
            ofp_parser = datapath.ofproto_parser
            req = ofp_parser.OFPFeaturesRequest(datapath)
            datapath.send_msg(req)


    #Register switches and determine OpenFlow version
    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def _handle_ConnectionUp(self, ev):
        self.observe_event(ofp_event.EventOFPPacketIn)
        msg = ev.msg
        datapath = msg.datapath
        self.ofp_version = msg.version
        if msg.version not in self.supported_protocols[OPENFLOW_PROTO]:
            self.supported_protocols[OPENFLOW_PROTO].append(msg.version)
        if datapath not in self.switches:
            self.switches[datapath.id] = datapath

    #Listen for switch features even after initial config state (in case new ones are connected)
    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, MAIN_DISPATCHER)
    def _handle_ConnectionUpMain(self, ev):
        self._handle_ConnectionUp(ev)

    #Main loop that listens to registered events from switches
    def _event_loop(self):
        #Register all events before continuing to listen to switches
        for event in ofp_event._OFP_MSG_EVENTS:
            self.observe_event(ofp_event._OFP_MSG_EVENTS[event])
        while self.is_active or not self.events.empty():
            ev, state = self.events.get()
            if ev == self._event_stop:
                continue
            handlers = self.get_handlers(ev, state)
            for handler in handlers:
                handler(ev)
            #Send the message to connected backend clients
            self.send_to_clients(ev)

    #Sends the message to the connected NetIDE clients
    def send_to_clients(self, ev):
        msg = ev.msg
        self.datapath = ev.msg.datapath
        #Add all the switches connected datapath.id and the connection information to the local variable
        if self.datapath.id not in self.switches:
            self.switches[self.datapath.id] = self.datapath

        #Encapsulate the feature request and send to connected client
        msg_to_send = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', None, None, self.datapath.id, str(msg.buf))
        #Forward the message to all the connected NetIDE clients

        print (':'.join(x.encode('hex') for x in msg_to_send))
        self.CoreConnection.socket.send(msg_to_send)

