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
import asyncore
import socket
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
sys.path.append('/home/doriguzzi/Projects/NetIDE/git_repository/Engine/ryu-shim/netide')
from netip import *

#Hadles the connection with the client. One backend channel is created per client.
class BackendChannel(asyncore.dispatcher):


    def __init__(self, sock, server):
        self.server = server
        self.channel_lock = threading.Lock()
        self.client_info = {'negotiated_protocols':{}}
        asyncore.dispatcher.__init__(self, sock)

    def handle_read(self):

        #If new client is connecting
        if 'connected' not in self.client_info:
            with self.channel_lock:
                header = self.recv(NetIDE_Header_Size)
                decoded_header = NetIDEOps.netIDE_decode_header(header)
                message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
                if decoded_header[NetIDEOps.NetIDE_header['VERSION']] is not NetIDEOps.NetIDE_version or decoded_header[NetIDEOps.NetIDE_header['TYPE']] is not NetIDEOps.NetIDE_type['NETIDE_HELLO']:
                    print "Attempt to connect from unsupported client"
                    self.recv(4096)

                    self.handle_close()
                else:
                    if message_length is 0:
                        print "Client does not support any protocol"
                        self.handle_close()
                        return

                    print "Client handshake received"
                    message_data = self.recv(message_length)
                    message_data = NetIDEOps.netIDE_decode_handshake(message_data, message_length)

                    #Find the common protocols that client and server support
                    count = 0
                    while count < message_length:
                        protocol = message_data[count]
                        version = message_data[count + 1]

                        if protocol in NetIDEOps.NetIDE_supported_protocols:
                            if version in NetIDEOps.NetIDE_supported_protocols[protocol]:
                                if protocol not in self.client_info['negotiated_protocols']:
                                    self.client_info['negotiated_protocols'][protocol] = {version}
                                else:
                                    self.client_info['negotiated_protocols'][protocol].add(version)

                        count += 2
                    self.client_info['connected'] = True
                    #After protocols have been negotiated, send back message to client to notify for common protocols
                    proto_data = NetIDEOps.netIDE_encode_handshake(self.client_info['negotiated_protocols'])
                    msg = NetIDEOps.netIDE_encode('NETIDE_HELLO', None, None, None, proto_data)
                    self.send(msg)
                    print "Server handshake sent"

        #If client has already performed handshake with the server
        else:
            header = self.recv(NetIDE_Header_Size)
            decoded_header = NetIDEOps.netIDE_decode_header(header)
            message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
            message_data = self.recv(message_length)
            if decoded_header[NetIDEOps.NetIDE_header['VERSION']] is not NetIDEOps.NetIDE_version or decoded_header[NetIDEOps.NetIDE_header['TYPE']] is not NetIDEOps.NetIDE_type['NETIDE_OPENFLOW']:
                print "Attempt to connect from unsupported client"
                self.recv(4096)
                self.handle_close()
            else:
                if message_length is 0:
                    print "Client does not support any protocol"
                    self.handle_close()
                    return
                #If datapath id is not 0, broadcast?
                if decoded_header[NetIDEOps.NetIDE_header['DPID']] is not 0:
                    self.datapath = self.server.controller.switches[int(decoded_header[NetIDEOps.NetIDE_header['DPID']])]
                    self.datapath.send(message_data)
                else:
                    self.datapath = None

    def handle_error(self):
        pass

    def handle_close(self):
        if self in self.server.clients:
            self.server.clients.remove(self)
            print "Client disconnected"
            self.close()


#Backend Server - Listens for incoming connections to the server and handles them properly
class BackendServer(asyncore.dispatcher):

    def __init__(self, controller, address):
        asyncore.dispatcher.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.set_reuse_addr()
        self.bind(address)
        self.listen(5)
        self.controller = controller
        self.clients = []
        return

    #Once a new connection is set up, create a channel with the client to be able to send/receive data
    def handle_accept(self):
        # Called when a backend connects to our socket
        backend_info = self.accept()

        if backend_info is not None:
            sock, addr = backend_info
            print "Incoming connection from client: %s" % repr(addr)
            client = BackendChannel(sock, self)
            self.clients.append(client)

            #Resend request for features for the new client
            self.controller.send_features_request()

        return


#Starts the asyncore server as a threaded process so the application can continue running
class asyncore_loop(threading.Thread):
        def run(self):
            asyncore.loop()

class RYUShim(app_manager.RyuApp):
    def __init__(self, *args, **kwargs):
        self.__class__.name = "RYUShim"
        super(RYUShim, self).__init__(*args, **kwargs)
        #Various Variables that can be edited
        __LISTEN_IP__ = 'localhost'
        __LISTEN_PORT__ = RYU_BACKEND_PORT #Default Port 41414

        #Internal variables
        self.switches = {}
        self.ofp_version = None

        #Default listen IP and port
        address = (__LISTEN_IP__, __LISTEN_PORT__)

        #Start the asyncore loop (Server) to listen to incomming connections
        print('RYUShim initiated')
        self.backend_server = BackendServer(self,address)
        self.al = asyncore_loop()
        self.al.start()


    #Explicitly sends a feature request to all the switches
    #OF Only? To check for other protocols!
    def send_features_request(self):
        for datapath_id, datapath in self.switches.iteritems():
            ofp_parser = datapath.ofproto_parser
            req = ofp_parser.OFPFeaturesRequest(datapath)
            datapath.send_msg(req)
    '''
    #Forwards all messags recieved from the switches to the clients
    def _event_loop(self):
        #First hello message from switch to get the version for negotiation
        ev, state = self.events.get()
        self.ofp_version = ev.msg.version
        NetIDEOps.NetIDE_supported_protocols[0x11] = {self.ofp_version}
        if ev.msg.datapath.id not in self.switches:
            self.switches[ev.msg.datapath.id] = ev.msg.datapath
        while self.is_active:
            ev, state = self.events.get()
            if ev.msg.datapath.id not in self.switches:
                self.switches[ev.msg.datapath.id] = ev.msg.datapath
            if ev == self._event_stop:
                continue
            self.send_to_clients(ev)
    '''
            

    #Register switches and determine OpenFlow version
    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def _handle_ConnectionUp(self, ev):
        self.observe_event(ofp_event.EventOFPPacketIn)
        msg = ev.msg
        datapath = msg.datapath
        self.ofp_version = msg.version
        NetIDEOps.NetIDE_supported_protocols[0x11] = {self.ofp_version}
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
        for client in self.backend_server.clients:
            client.send(msg_to_send)

