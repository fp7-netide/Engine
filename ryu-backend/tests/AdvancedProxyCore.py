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
    def __init__(self, port,spec):
        threading.Thread.__init__(self)
        self.kill_received = False
        self.composition = Composition(spec)
        self.port = port

    def run(self):
        context = zmq.Context()
        socket = context.socket(zmq.ROUTER)
        socket.bind("tcp://*:" + self.port)
        logger.debug("Bound port %s", self.port)
    
        connected_backends = []
        running_modules = {}
        module_id = 1
    
        while not self.kill_received:
            (identity, message) = socket.recv_multipart()
            if "backend" in identity:
                # Handle the backend socket
                logger.debug("\nMessage from client controller ID %s", identity)
                decoded_header = NetIDEOps.netIDE_decode_header(message)
                logger.debug("Message header: %s", decoded_header)
                logger.debug("Message type: %s", NetIDEOps.key_by_value(NetIDEOps.NetIDE_type,decoded_header[NetIDEOps.NetIDE_header['TYPE']]))
                message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
                message_data = message[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
                logger.debug("Message body: %s",':'.join(x.encode('hex') for x in message_data))
                # Forwarding the message to the shim
                if identity not in connected_backends:
                    connected_backends.append(identity)
    
                if decoded_header[NetIDEOps.NetIDE_header['TYPE']] is NetIDEOps.NetIDE_type['MODULE_ANNOUNCEMENT']:
                    ack_message = NetIDEOps.netIDE_encode('MODULE_ACKNOWLEDGE', decoded_header[NetIDEOps.NetIDE_header['XID']], module_id, None, message_data)
                    running_modules[message_data] = {'module_id':module_id, 'backend':identity }
                    module_id += 1
                    socket.send_multipart([identity,ack_message])
                else:
                    socket.send_multipart([shimname,message])
    
            elif "shim" in identity:
                # Get next client request, route to last-used worker
                logger.debug("\n-----------Message from server controller ID %s", identity)
                decoded_header = NetIDEOps.netIDE_decode_header(message)
                logger.debug("Message header: %s", decoded_header)
                message_type = NetIDEOps.key_by_value(NetIDEOps.NetIDE_type,decoded_header[NetIDEOps.NetIDE_header['TYPE']])
                logger.debug("Message type: %s", message_type)
                message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
                message_data = message[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
                logger.debug("Message body: %s",':'.join(x.encode('hex') for x in message_data))
                # Forwarding the message to all the backends
                datapath_id = decoded_header[NetIDEOps.NetIDE_header['DPID']]
                
                # only perform composition/conflict resolution for control messages. We do not care of management messages.
                if decoded_header[NetIDEOps.NetIDE_header['TYPE']] >= NetIDEOps.NetIDE_type['NETIDE_OPENFLOW']:
                    #these are the modules that can handle the event
                    modules = self.composition.check_event_conditions(hex(datapath_id), message_data)    
                    for module in modules:
                        running_module = running_modules.get(module,None)
                        if running_module is not None:
                            backend_identity = running_module.get('backend',None)
                            module_id = running_module.get('module_id',0)
                            if backend_identity is not None:
                                socket.send_multipart([backend_identity,NetIDEOps.netIDE_set_module_id(message, module_id)])
                else:
                    for backendname in connected_backends:
                        socket.send_multipart([backendname,message])
    
        # Clean up
        socket.close()
        context.term()

def main(argv):

    port = '5555'
    composition_specification = 'CompositionSpecification.xml'
    
    try:
        opts, args = getopt.getopt(argv,"hl:p:c:",["port=","cspec="])
    except getopt.GetoptError:
        print 'AdvancedCoreProxy.py -p <port> -c <composition_specification>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print 'AdvancedCoreProxy.py -p <port> -c <composition_specification> -l <log_level (notset, debug)>'
            sys.exit()
        elif opt in ("-l"):
            log_level = LEVELS.get(arg, logging.NOTSET)
            logger.setLevel(log_level)
        elif opt in ("-p", "--port"):
            port = arg
        elif opt in ("-c", "--cspec"):
            composition_specification = arg

    message_dispatcher = MessageDispatcher(port, composition_specification)
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
        