################################################################################
# NetIDE Core - Python implementation                                          #
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

import zmq
import sys
import time
import getopt
import threading
import logging
from lxml import etree
from ryu.netide.netip import *


shimname = "shim"
logger = logging.getLogger()
handler = logging.StreamHandler()
formatter = logging.Formatter(
        '%(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

LEVELS = {  'notset':logging.NOTSET,
            'debug':logging.DEBUG,
            'info':logging.INFO,
            'warning':logging.WARNING,
            'error':logging.ERROR,
            'critical':logging.CRITICAL,
            }

class Composition():
    def __init__(self, spec_filename):
        self.doc = etree.parse(spec_filename)
        self.module_list = {}
        self.load_modules(self.doc)
    
    # parsing the composition configuration
    def load_modules(self, doc):
        for df in doc.xpath('//Module'):
            self.module_list[df.attrib['id']] = {}
            module = self.module_list[df.attrib['id']]
            for sf in df.getchildren():
                for key in sf.attrib:
                    module[key] = sf.attrib[key].split()
    
    # this method returns the modules that are entitled to receive the event message
    def check_event_conditions(self, datapath_id, control_message):
        modules = [] 
        for module, conditions in self.module_list.iteritems():
            for dpid in conditions['datapaths']:
                if int(datapath_id,16) == int(dpid,16):
                    modules.append(module)
        return modules

# Handles the communication between the core, the shim and the backends
class MessageDispatcher(threading.Thread):
    def __init__(self, port,timeout,spec):
        threading.Thread.__init__(self)
        self.kill_received = False
        self.composition = Composition(spec)
        self.port = port
        self.heartbeat_timeout = timeout
        self.heartbeat_time = time.time()
        self.connected_backends = {}
        self.running_modules = {}

    def check_heartbeat(self):
        # checking the status of the previously sent messages
        if time.time() > self.heartbeat_time + self.heartbeat_timeout:
            self.heartbeat_time = time.time()
            for backendname, prop in self.connected_backends.items():
                # backends that not appeared for twice the self.heartbeat_timeout are deleted
                if time.time() > prop['hb_time'] + self.heartbeat_timeout*2:
                    del self.connected_backends[backendname]
                    for modulename, modprop in self.running_modules.items():
                        if modprop['backend'] == backendname:
                            del self.running_modules[modulename]

            logger.debug("Connected Backends: %s", self.connected_backends)
            logger.debug("Running Modules: %s", self.running_modules)


    def run(self):
        context = zmq.Context()
        socket = context.socket(zmq.ROUTER)
        socket.bind("tcp://*:" + self.port)
        logger.debug("Bound port %s", self.port)
        
        # Initialize main loop state
        poller = zmq.Poller()
        # Only poll for requests from backend until workers are available
        poller.register(socket, zmq.POLLIN)
        module_id = 1
    
        while not self.kill_received:
            sockets = dict(poller.poll(self.heartbeat_timeout*1000))
            self.check_heartbeat()

            if socket in sockets:
                (identity, message) = socket.recv_multipart()
                logger.debug("\n-----------Message from ID %s", identity)
                decoded_header = NetIDEOps.netIDE_decode_header(message)
                logger.debug("Message header: %s", decoded_header)
                message_type = NetIDEOps.key_by_value(NetIDEOps.NetIDE_type,decoded_header[NetIDEOps.NetIDE_header['TYPE']])
                logger.debug("Message type: %s", message_type)
                message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
                message_data = message[NetIDEOps.NetIDE_Header_Size:]
                logger.debug("Message body: %s",':'.join(x.encode('hex') for x in message_data))

                if "backend" in identity:
                    # Recording the new backend
                    if identity not in self.connected_backends:
                        self.connected_backends[identity] = {'id': -1, 'hello' : -1, 'hb_time' : time.time()}

                    # any message received from a backend updates the heartbeat time
                    backend = self.connected_backends[identity]
                    backend['hb_time'] = time.time()

                    if message_type is 'NETIDE_HEARTBEAT':
                        # heartbeat time already updated, go back to the while
                        continue
                    # Handling the message
                    elif message_type is 'MODULE_ANNOUNCEMENT':
                        ack_message = NetIDEOps.netIDE_encode('MODULE_ACKNOWLEDGE', decoded_header[NetIDEOps.NetIDE_header['XID']], module_id, None, message_data)
                        self.running_modules[message_data] = {'module_id': module_id, 'backend': identity}
                        backend = self.connected_backends.get(message_data)
                        if backend is not None:
                            backend['id'] = module_id
                        module_id += 1
                        socket.send_multipart([identity,ack_message])
                    else:
                        socket.send_multipart([shimname,message])
        
                elif "shim" in identity:
                    # Forwarding the message to all the backends
                    datapath_id = decoded_header[NetIDEOps.NetIDE_header['DPID']]
                    
                    # only perform composition/conflict resolution for control messages. We do not care of management messages.
                    if decoded_header[NetIDEOps.NetIDE_header['TYPE']] >= NetIDEOps.NetIDE_type['NETIDE_OPENFLOW']:
                        #these are the modules that can handle the event
                        modules = self.composition.check_event_conditions(hex(datapath_id), message_data)    
                        for module in modules:
                            running_module = self.running_modules.get(module,None)
                            if running_module is not None:
                                backend_identity = running_module.get('backend',None)
                                module_id = running_module.get('module_id',0)
                                if backend_identity is not None:
                                    backend = self.connected_backends.get(backend_identity,None)
                                    if backend['hello'] > 0: #hello completed between shim and backend. We can start sending messages to this backend
                                        socket.send_multipart([backend_identity,NetIDEOps.netIDE_set_module_id(message, module_id)])
                    # handling management messages (no control messages here)
                    else:
                        module_id = decoded_header[NetIDEOps.NetIDE_header['MOD_ID']]
                        for backendname, prop in self.connected_backends.items():
                            if prop['id'] is module_id:
                                if decoded_header[NetIDEOps.NetIDE_header['TYPE']] == NetIDEOps.NetIDE_type['NETIDE_HELLO'] and message_length > 0:
                                    socket.send_multipart([backendname,message])
                                    prop['hello'] = 1
                                elif prop['hello'] > 0: #hello completed between shim and backend. We can start sending messages to this backend
                                    socket.send_multipart([backendname,message])
                                break
        
        # Clean up
        socket.close()
        context.term()

def main(argv):

    port = '5555'
    heartbeat_timeout = 5 # seconds 
    composition_specification = 'CompositionSpecification.xml'
    
    try:
        opts, args = getopt.getopt(argv,"hl:p:c:t:",["port=","cspec=","timeout="])
    except getopt.GetoptError:
        print 'AdvancedCoreProxy.py -p <port> -c <composition_specification> -t <heartbeat_timeout>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print 'AdvancedCoreProxy.py -p <port> -c <composition_specification> -l <log_level (notset, debug)> -t <heartbeat_timeout (secs)>'
            sys.exit()
        elif opt in ("-l"):
            log_level = LEVELS.get(arg, logging.NOTSET)
            logger.setLevel(log_level)
        elif opt in ("-t", "--timeout"):
            heartbeat_timeout = arg
        elif opt in ("-p", "--port"):
            port = arg
        elif opt in ("-c", "--cspec"):
            composition_specification = arg

    message_dispatcher = MessageDispatcher(port, heartbeat_timeout, composition_specification)
    message_dispatcher.setDaemon(True)
    message_dispatcher.start()
    
    while message_dispatcher.is_alive():
        try:
            message_dispatcher.join(1)
        except KeyboardInterrupt:
            print "Ctrl-C received! Sending kill to thread..."
            message_dispatcher.kill_received = True
    

if __name__ == "__main__":
    main(sys.argv[1:])
        
