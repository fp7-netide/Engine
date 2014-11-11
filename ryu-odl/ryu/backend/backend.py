
################################################################################
# The Pyretic Project                                                          #
# frenetic-lang.org/pyretic                                                    #
# author: Joshua Reich (jreich@cs.princeton.edu)                               #
################################################################################
# Licensed to the Pyretic Project by one or more contributors. See the         #
# NOTICES file distributed with this work for additional information           #
# regarding copyright and ownership. The Pyretic Project licenses this         #
# file to you under the following license.                                     #
#                                                                              #
# Redistribution and use in source and binary forms, with or without           #
# modification, are permitted provided the following conditions are met:       #
# - Redistributions of source code must retain the above copyright             #
#   notice, this list of conditions and the following disclaimer.              #
# - Redistributions in binary form must reproduce the above copyright          #
#   notice, this list of conditions and the following disclaimer in            #
#   the documentation or other materials provided with the distribution.       #
# - The names of the copyright holds and contributors may not be used to       #
#   endorse or promote products derived from this work without specific        #
#   prior written permission.                                                  #
#                                                                              #
# Unless required by applicable law or agreed to in writing, software          #
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT    #
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the     #
# LICENSE file distributed with this work for specific language governing      #
# permissions and limitations under the License.                               #
################################################################################

import time
import threading
import contextlib
from ryu.ofproto import ofproto_common
from ryu.ofproto import ofproto_parser
from ryu.ofproto import ofproto_protocol
from ryu.ofproto import ofproto_parser
from ryu.ofproto import ofproto_v1_0
from ryu.ofproto import ofproto_v1_0_parser
from ryu.ofproto.ofproto_v1_0_parser import *
from ryu.lib.packet import *
from ryu.lib.dpid import dpid_to_str
from ryu.lib import hub
import random
from ryu.ofproto import nx_match
from ryu.controller import ofp_event
from ryu.controller import handler
from ryu.backend.comm import * 
import ryu.base.app_manager
from ryu.controller.handler import HANDSHAKE_DISPATCHER, CONFIG_DISPATCHER,\
    MAIN_DISPATCHER
    
ofp_port_features_rev_map={
  'OFPPF_10MB_HD'    : 1,
  'OFPPF_10MB_FD'    : 2,
  'OFPPF_100MB_HD'   : 4,
  'OFPPF_100MB_FD'   : 8,
  'OFPPF_1GB_HD'     : 16,
  'OFPPF_1GB_FD'     : 32,
  'OFPPF_10GB_FD'    : 64,
  'OFPPF_COPPER'     : 128,
  'OFPPF_FIBER'      : 256,
  'OFPPF_AUTONEG'    : 512,
  'OFPPF_PAUSE'      : 1024,
  'OFPPF_PAUSE_ASYM' : 2048,
}

class Backend(ryu.base.app_manager.RyuApp):

    class asyncore_loop(threading.Thread):
        def run(self):
            asyncore.loop()

    def __init__(self):
        super(Backend, self).__init__()
        self.name = 'ryu_backend'
        self.backend_channel = None
        self.runtime = None
        self.channel_lock = threading.Lock()
        print "Ryu Backend init"
        address = ('localhost', RYU_BACKEND_PORT) # USE KNOWN PORT
        self.backend_server = BackendServer(self,address)
        
        self.al = self.asyncore_loop()
        self.al.daemon = True
        self.al.start()
        
    def send_to_OF_client(self,msg):
        print "Send to OF client"
        serialized_msg = serialize(msg)
        #print "serialized message", serialized_msg
        
        with self.channel_lock:
            if not self.backend_channel is None:
                self.backend_channel.push(serialized_msg)
        
    def send_packet(self,packet):
        self.send_to_OF_client(['packet',packet])

    def send_install(self,pred,priority,action_list):
        self.send_to_OF_client(['install',pred,priority,action_list])

    def send_delete(self,pred,priority):
        self.send_to_OF_client(['delete',pred,priority])
        
    def send_clear(self,switch):
        self.send_to_OF_client(['clear',switch])

    def send_flow_stats_request(self,switch):
        self.send_to_OF_client(['flow_stats_request',switch])

    def send_barrier(self,switch):
        self.send_to_OF_client(['barrier',switch])

    def inject_discovery_packet(self,dpid, port):
        self.send_to_OF_client(['inject_discovery_packet',dpid,port])
                
    def send_msg_from_ryu(self,msg):
        print "Backend: send_msg_from_ryu" , msg
        if msg is None or msg.msg_len == 0:
            print "ERROR: empty message"
        elif msg.msg_type == ofproto.OFPT_HELLO:
            print "message type: ", msg.msg_type 
        elif msg.msg_type == ofproto.OFPT_FEATURES_REQUEST:
            print "message type: ", msg.msg_type
        return

class BackendServer(asyncore.dispatcher):
    """Receives connections and establishes handlers for each backend.
    """
    def __init__(self, backend, address):
        asyncore.dispatcher.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.set_reuse_addr()
        self.bind(address)
        self.address = self.socket.getsockname()
        self.listen(1)
        self.backend = backend
        return

    def handle_accept(self):
        # Called when a backend connects to our socket
        backend_info = self.accept()
        print "OpenFlow client connected: ", backend_info
        self.backend.backend_channel = BackendChannel(self.backend,sock=backend_info[0])
        # We only want to deal with one backend at a time,
        # so close as soon as we set up the handler.
        # Normally you would not do this and the server
        # would run forever or until it received instructions
        # to stop.
        self.handle_close()
        return
    
    def handle_close(self):
        self.close()
        
class BackendChannel(asynchat.async_chat):
    """Handles echoing messages from a single backend.
    """
    def __init__(self, backend, sock):
        self.backend = backend
        self.received_data = []
        self.datapaths = {}
        asynchat.async_chat.__init__(self, sock)
        self.ac_in_buffer_size = 4096 * 3
        self.ac_out_buffer_size = 4096 * 3
        self.set_terminator(TERM_CHAR)
        return

    def collect_incoming_data(self, data):
        """Read an incoming message from the backend and put it into our outgoing queue."""
        with self.backend.channel_lock:
            self.received_data.append(data)

    def found_terminator(self):
        """The end of a command or message has been seen."""
        with self.backend.channel_lock:
            msg = deserialize(self.received_data)

        # USE DESERIALIZED MSG
        if msg is None or len(msg) == 0:
            print "ERROR: empty message"
        elif msg[0] == 'switch':
            if msg[1] == 'join':
                if msg[3] == 'BEGIN':
                    print "Backend: Switch connected: ", msg
                    #TODO: i) manage the reconnection of a switch and ii) manage different versions of the protocol
                    datapath = BackendDatapath(msg[2], ofproto_v1_0, ofproto_v1_0_parser)
                    self.datapaths[datapath.id] = datapath
                    datapath.hello_handler()
                    for id in self.datapaths:
            			print "Datapath id: ", self.datapaths[id].id , " state: ", self.datapaths[id].state
            elif msg[1] == 'part':
                datapath = self.datapaths[msg[2]]
                if datapath:
                    del datapath
                    del self.datapaths[msg[2]]
            else:
                print "ERROR: Bad switch event"
        elif msg[0] == 'port':
            print "Backend: Port status: ", msg
            if msg[1] == 'join':
                datapath = self.datapaths[msg[2]]
                datapath.ports[msg[3]] = {'port_no':msg[3], 'config':msg[4],'state':msg[5],'curr':msg[6]}
                print "datapath ports: ", datapath.ports
            #elif msg[1] == 'mod':
            #    self.backend.runtime.handle_port_mod(msg[2],msg[3],msg[4],msg[5],msg[6])
            #elif msg[1] == 'part':
            #    self.backend.runtime.handle_port_part(msg[2],msg[3])
            #else:
            #    print "ERROR: Bad port event"
        elif msg[0] == 'link':
            print "Backend: Link update: ", msg
            #self.backend.runtime.handle_link_update(msg[1],msg[2],msg[3],msg[4])
        elif msg[0] == 'packet':
            data = msg[1]
            datapath = self.datapaths[data['switch']]
            datapath.packet_in_handler(data)
            #self.backend_manager.Handle_PacketIn(packet)
        elif msg[0] == 'flow_stats_reply':
            print "Backend: Flow stat reply: ", msg
           # self.backend.runtime.handle_flow_stats_reply(msg[1],msg[2])
        else:
            print 'ERROR: Unknown msg from backend %s' % msg
        return
        
class BackendDatapath(ofproto_protocol.ProtocolDesc):
    def __init__(self, id, ofp, ofpp):
        super(BackendDatapath, self).__init__()
        
        self.is_active = True
        self.ofproto = ofp
        self.ofproto_parser = ofpp
        self.xid = random.randint(0, self.ofproto.MAX_XID)
        self.id = id
        self.xid = 0
        self.ports = {}
        self.ofp_brick = ryu.base.app_manager.lookup_service_brick('ofp_event')
        self.set_state(handler.HANDSHAKE_DISPATCHER)
        self.condition = threading.Condition()
    
    def to_hex_string(self,value,bytes):
        encoded = format(value, 'x')
     	length = len(encoded)
      	encoded = encoded.zfill(length+length%2)
      	decoded = encoded.decode('hex')
      	length = len(encoded)/2
      	if length < bytes:
      		decoded = '\x00' * (bytes-length) + decoded
     	return decoded
  
    def packet_from_network(self, **kwargs):
        return kwargs

    def packet_to_network(self, packet):
        return packet['raw']
        
    def close(self):
    	print "Close Datapath: ", self.id
        #self.set_state(handler.DEAD_DISPATCHER)

    def set_state(self, state):
        self.state = state
        ev = ofp_event.EventOFPStateChange(self)
        ev.state = state
        self.ofp_brick.send_event_to_observers(ev, state)
    
    def get_xid(self):
        self.xid += 1
        self.xid &= self.ofproto.MAX_XID
        return self.xid

    def set_xid(self, msg):
        self.xid += 1
        self.xid &= self.ofproto.MAX_XID
        msg.set_xid(self.xid)
        return self.xid
        
    def send_msg(self, msg):
        assert isinstance(msg, self.ofproto_parser.MsgBase)
        if msg.xid is None:
            self.set_xid(msg)
        msg.serialize()
        
        if msg.msg_type == ofproto.OFPT_FEATURES_REQUEST:
            t = threading.Thread(target=self.switch_features_handler, args=(msg,))
            t.start()
            #self.switch_features_handler(msg)
        
        
    def hello_handler(self):
        version = ofproto.OFP_VERSION
        msg_type = ofproto.OFPT_HELLO
        msg_len = ofproto.OFP_HEADER_SIZE
        data = '\x00' * 4  
        xid = self.get_xid()

        fmt = ofproto.OFP_HEADER_PACK_STR
        buf = struct.pack(fmt, version, msg_type, msg_len, xid) + data
        hello = OFPHello.parser(self, version, msg_type, msg_len, xid,
                              bytearray(buf))
        
        print "Datapath: ", self.id, " hello message: ", hello
        self.register_event(hello)
    
    def switch_features_handler(self,msg):     
        # TODO: missing details from the client
        # 1. port hw address
        # 2. port name
        # workaorund that waits until the state is updated to 'config'
        self.condition.acquire()
        while not self.state == CONFIG_DISPATCHER:
            self.condition.wait(0.1)  
        self.condition.release()
        
        version = ofproto.OFP_VERSION
        msg_type = ofproto.OFPT_FEATURES_REPLY
        msg_len = ofproto.OFP_SWITCH_FEATURES_SIZE + ofproto.OFP_PHY_PORT_SIZE * len(self.ports)
        xid = msg.xid   
        zfill = '\x00' * 3    
        
        data = self.to_hex_string(version,1) \
            + self.to_hex_string(msg_type,1) \
            + self.to_hex_string(msg_len,2) \
            + self.to_hex_string(xid,4) \
            + self.to_hex_string(self.id,8) \
            + self.to_hex_string(0xFF,4) \
            + self.to_hex_string(0xFF,1) \
            + zfill \
            + self.to_hex_string(0xFFFFFFFF,4) \
            + self.to_hex_string(0xFFFFFFFF,4)  
        
        # adding ports  
        for port_no in self.ports:
            port_curr = 0
            for curr_str in self.ports[port_no]['curr']:
                port_curr = port_curr + ofp_port_features_rev_map.get(curr_str)
            
            data = data + \
            self.to_hex_string(self.ports[port_no]['port_no'], 2) \
            + addrconv.mac.text_to_bin('00:00:00:00:00:00') \
            + 'port-' + str(self.ports[port_no]['port_no']).rjust(11,'0') \
            + self.to_hex_string(self.ports[port_no]['config'],4) \
            + self.to_hex_string(self.ports[port_no]['state'],4) \
            + self.to_hex_string(port_curr,4) \
            + self.to_hex_string(0xFFFFFFFF,4) \
            + self.to_hex_string(0xFFFFFFFF,4) \
            + self.to_hex_string(0xFFFFFFFF,4)
        
        fmt = ofproto.OFP_SWITCH_FEATURES_PACK_STR
        #buf = struct.pack(fmt, version, msg_type, msg_len, xid, data)
        features_reply = OFPSwitchFeatures.parser(self, version, msg_type, msg_len, xid, data)
        
        print "Datapath: ", self.id, " features_reply message: ", features_reply
        self.register_event(features_reply)
    
    def packet_in_handler(self,msg): 
        version = ofproto.OFP_VERSION
        msg_type = ofproto.OFPT_PACKET_IN
        msg_len = ofproto.OFP_PACKET_IN_SIZE
        in_port = msg['inport']
        buf = self.packet_to_network(msg)
        buf_len = len(buf)
        zfill = '\x00' * 1    
        
        # TODO: buffer_id is set to -1 since is not provided by the shim client
        data =  self.to_hex_string(version,1) \
                + self.to_hex_string(msg_type,1) \
                + self.to_hex_string(msg_len,2) \
                + self.to_hex_string(0,4) \
                + self.to_hex_string(0xFFFFFFFF,4) \
                + self.to_hex_string(msg_len + buf_len,2) \
                + self.to_hex_string(in_port,2) \
                + self.to_hex_string(ofproto.OFPR_NO_MATCH,1) \
                + zfill \
                + buf
        
        packet_in = ofproto_parser.msg(self, version, msg_type, msg_len, 0, data)
        print "Backend. Datapath: " , self.id, " packet_in:", packet_in
        self.register_event(packet_in)
    
    def register_event(self,msg):
        if msg:
            print "Datapath ", self.id, " current state 1: ", self.state, " message type: ", msg.msg_type
            ev = ofp_event.ofp_msg_to_ev(msg)        
            self.ofp_brick.send_event_to_observers(ev, self.state)
            dispatchers = lambda x: x.callers[ev.__class__].dispatchers
        	
            handlers = [handler for handler in
                        self.ofp_brick.get_handlers(ev) if
                        self.state in dispatchers(handler)]
            print "handlers: ", self.ofp_brick.get_handlers(ev)
            for handler in handlers:
                handler(ev)
                print "Datapath ", self.id, " current state 2: ", self.state, " message type: ", msg.msg_type