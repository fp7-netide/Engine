################################################################################
# Shim Layer for the Ryu Controller platform                                   #
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
import threading
import sys
import random
import binascii
import time
import socket
import zlib
from eventlet.green import zmq
from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller import dpset
from ryu.controller.handler import MAIN_DISPATCHER, set_ev_handler, set_ev_cls
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_parser
from ryu.netide.netip import *

NETIDE_CORE_PORT = 5555
XID_TIMEOUT = 5 # xid are removed from the database after 5 seconds
XID_CRC_INDEX = 0
XID_DB = {}


logger = logging.getLogger('ryu-shim')
logger.setLevel(logging.DEBUG)

def set_xid(of_array, xid):
    of_array[4]=(xid >>24) & 0xff;
    of_array[5]=(xid >>16) & 0xff;
    of_array[6]=(xid >>8) & 0xff;
    of_array[7]=xid & 0xff;
    return of_array

def store_xid(xid, backend_id):
    ret = True
    global XID_CRC_INDEX
    global XID_DB
    while ret is not False:
        new_xid = zlib.crc32(str(xid)+str(backend_id),XID_CRC_INDEX)
        new_xid = new_xid & 0xffffffff
        ret = XID_DB.get(new_xid,False)
        XID_CRC_INDEX += 1

    XID_DB[new_xid] = {'old_xid' : xid, 'backend_id' : backend_id, 'time' : time.time()}
    return new_xid

def get_xid(xid):
    global XID_DB
    old_values = XID_DB.get(xid,None)
    if old_values is None:
        return None
    else:
        del XID_DB[xid]
        return old_values

def purge_xid():
    global XID_DB
    global XID_TIMEOUT
    current_time = time.time()
    for xid, prop in XID_DB.items():
        if current_time>  prop['time'] + XID_TIMEOUT:
            del XID_DB[xid]

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
        logger.debug('Connecting to Core on %s:%s...', self.host, self.port)
        self.socket.connect("tcp://" +str(self.host) + ":" + str(self.port))

        #self.socket.send(b"First Hello from " + self.id)
        while True:
            message = self.socket.recv_multipart()
            msg = self.get_multipart_message(message)
            self.handle_read(msg)

        self.socket.close()
        context.term()
    
    
    def get_multipart_message(self,msg):
        for part in msg:
            if len(part) > 0:
                return part

    def handle_read(self,msg):
        decoded_header = NetIDEOps.netIDE_decode_header(msg)
        if decoded_header is False:
            return False
        logger.debug("Received from Core: Message header: %s", decoded_header)
        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
        message_data = msg[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
        logger.debug("Received from Core: Message body: %s",':'.join(x.encode('hex') for x in message_data))

        if decoded_header[NetIDEOps.NetIDE_header['VERSION']] is not NetIDEOps.NetIDE_version:
            print ("Attempt to connect from unsupported client")
            return
        else:
            #If new client is connecting
            if decoded_header[NetIDEOps.NetIDE_header['TYPE']] is NetIDEOps.NetIDE_type['NETIDE_HELLO']:
                
                if message_length is 0:
                    print ("WARNING: Client does not support any protocol")
                    return

                if self.controller.connection_up is False:
                    print ("WARNING: It seems that the server controller is not connected to the switches/Mininet")
                    return

                backend_id = decoded_header[NetIDEOps.NetIDE_header['MOD_ID']]
                logger.debug( "Received HELLO message from backend: ,%s", backend_id)
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
                if len(proto_data) == 0:
                    msg = NetIDEOps.netIDE_encode('NETIDE_ERROR', None, backend_id, None, None)
                    self.socket.send(msg)
                else:
                    msg = NetIDEOps.netIDE_encode('NETIDE_HELLO', None, backend_id, None, proto_data)
                    self.socket.send(msg)
                    #Resend request for features for the new client
                    self.controller.send_features_request(backend_id)

            elif decoded_header[NetIDEOps.NetIDE_header['TYPE']] is NetIDEOps.NetIDE_type['NETIDE_OPENFLOW']:
                purge_xid() # removes the old entries from the xid database

                if message_length is 0:
                    return

                if decoded_header[NetIDEOps.NetIDE_header['DPID']] is not 0:
                    self.datapath = self.controller.switches[int(decoded_header[NetIDEOps.NetIDE_header['DPID']])]

                    # Here we set a ""fake" xid so that the replies to request messages can be forwarded to the correct module by the core
                    (version, msg_type, msg_len, xid) = ofproto_parser.header(message_data)
                    module_id = decoded_header[NetIDEOps.NetIDE_header['MOD_ID']]
                    if module_id is not None:
                        new_xid = store_xid(xid,module_id)
                        ret = bytearray(message_data)
                        set_xid(ret,new_xid)
                        message_data = str(ret)

                    self.datapath.send(message_data)
                else:
                    self.datapath = None

    
    
class RyuShim(app_manager.RyuApp):
    def __init__(self, *args, **kwargs):
        self.__class__.name = "RYUShim"
        super(RyuShim, self).__init__(*args, **kwargs)

        # Various Variables that can be edited
        __CORE_IP__ = 'localhost'
        __CORE_PORT__ = NETIDE_CORE_PORT

        # Internal variables
        self.switches = {}
        #self.shim_id = b"shim-ryu-" + str(os.getpid())
        self.shim_id = b"shim"
        self.connection_up = False
        self.supported_protocols = {}
        self.supported_protocols[OPENFLOW_PROTO] = []
        self.supported_protocols[NETCONF_PROTO] = []
        self.ofp_version = None

        # Start the connection to the core
        self.CoreConnection = CoreConnection(self, self.shim_id, __CORE_IP__,__CORE_PORT__)
        self.CoreConnection.setDaemon(True)
        self.CoreConnection.start()

    #Explicitly sends a feature request to all the switches
    #OF Only? To check for other protocols!
    def send_features_request(self,backend_id):
        for datapath_id, datapath in self.switches.iteritems():
            ofp_parser = datapath.ofproto_parser
            req = ofp_parser.OFPFeaturesRequest(datapath)
            req.xid = store_xid(backend_id,backend_id)
            datapath.send_msg(req)

    def add_flow10(self, datapath, match, actions, idle_to, hard_to):
        ofproto = datapath.ofproto
        mod = datapath.ofproto_parser.OFPFlowMod(
            datapath=datapath, match=match, cookie=0,
            command=ofproto.OFPFC_ADD, idle_timeout=idle_to, hard_timeout=hard_to,
            priority=ofproto.OFP_DEFAULT_PRIORITY,
            flags=ofproto.OFPFF_SEND_FLOW_REM, actions=actions)
        datapath.send_msg(mod)

    def add_flow(self, datapath, priority, match, actions, idle_to=0, hard_to=0, buffer_id=None):
        ofproto = datapath.ofproto
        parser = datapath.ofproto_parser
        inst = [parser.OFPInstructionActions(ofproto.OFPIT_APPLY_ACTIONS,actions)]
        if buffer_id:
            mod = parser.OFPFlowMod(datapath=datapath, buffer_id=buffer_id,
                                    priority=priority, match=match,
                                    instructions=inst6)
        else:
            mod = parser.OFPFlowMod(datapath=datapath, priority=priority,
                                    match=match, instructions=inst, idle_timeout=idle_to, hard_timeout=hard_to)
        datapath.send_msg(mod)

    #Register switches and determine OpenFlow version
    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def _handle_ConnectionUp(self, ev):
        self.observe_event(ofp_event.EventOFPPacketIn)
        self.connection_up = True
        msg = ev.msg
        datapath = msg.datapath
        self.ofp_version = msg.version
        if msg.version not in self.supported_protocols[OPENFLOW_PROTO]:
            self.supported_protocols[OPENFLOW_PROTO].append(msg.version)
        if datapath not in self.switches:
            self.switches[datapath.id] = datapath

        # if ofp_version >= OF1.3, then install the table-miss default behavior
        if self.ofp_version >= 0x04:
            ofproto = datapath.ofproto
            parser = datapath.ofproto_parser
            match = parser.OFPMatch()
            actions = [parser.OFPActionOutput(ofproto.OFPP_CONTROLLER, ofproto.OFPCML_NO_BUFFER)]
            self.add_flow(datapath, 0, match, actions)

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
            #Send the message to connected backend clients. We try to restore the old xid and module_id in case of reply messages
            msg = ev.msg
            type = msg.msg_type
            datapath = ev.msg.datapath
            buf = bytearray(msg.buf)
            module_id = None

            if type not in async_messages:
                old_values = get_xid(msg.xid)
                if old_values is not None:
                    module_id = old_values['backend_id']
                    set_xid(buf,old_values['old_xid'])


            #Hello messages are not sent to the core
            if type is not datapath.ofproto.OFPT_HELLO:
                self.send_to_clients(datapath, str(buf),module_id)

    #Sends the message to the connected NetIDE clients
    def send_to_clients(self, datapath, msg_buf, module_id):

        #Add all the switches connected datapath.id and the connection information to the local variable
        if datapath.id not in self.switches:
            self.switches[datapath.id] = datapath

        #Encapsulate the feature request and send to connected client
        msg_to_send = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', None, module_id, datapath.id, str(msg_buf))
        #Forward the message to all the connected NetIDE clients
        decoded_header = NetIDEOps.netIDE_decode_header(msg_to_send)
        logger.debug("Sending to Core: Message header: %s", decoded_header)
        message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
        message_data = msg_to_send[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
        logger.debug("Sending to Core: Message body: %s",':'.join(x.encode('hex') for x in message_data))
        self.CoreConnection.socket.send(msg_to_send)

