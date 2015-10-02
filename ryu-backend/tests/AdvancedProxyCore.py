import zmq
import sys
import getopt
from netip import *


shimname = "shim"

def main(argv):

    shim_port = '41414'
    backend_port = '51515'

    try:
        opts, args = getopt.getopt(argv,"hs:b:",["sport=","bport="])
    except getopt.GetoptError:
        print 'AdvancedCoreProxy.py -s <shim_port> -b <backends_port>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print 'AdvancedCoreProxy.py -s <shim_port> -b <backends_port>'
            sys.exit()
        elif opt in ("-s", "--sport"):
            shim_port = arg
        elif opt in ("-b", "--bport"):
            backend_port = arg

    context = zmq.Context.instance()
    shim = context.socket(zmq.ROUTER)
    shim.bind("tcp://*:" + shim_port)
    print "Bound port ", shim_port, " for the server controller"
    backend = context.socket(zmq.ROUTER)
    backend.bind("tcp://*:" + backend_port)
    print "Bound port ", backend_port, " for the client controllers"

    # Initialize main loop state
    poller = zmq.Poller()
    # Only poll for requests from backend until workers are available
    poller.register(backend, zmq.POLLIN)
    poller.register(shim, zmq.POLLIN)

    connected_backends = []
    running_modules = {}
    module_id = 1

    while True:
        sockets = dict(poller.poll())

        if backend in sockets:
            # Handle the backend socket
            (identity, message) = backend.recv_multipart()
            print "\nMessage from client controller ID ", identity
            decoded_header = NetIDEOps.netIDE_decode_header(message)
            print "Message header: ", decoded_header
            print "Message type: ", NetIDEOps.key_by_value(NetIDEOps.NetIDE_type,decoded_header[NetIDEOps.NetIDE_header['TYPE']])
            message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
            message_data = message[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
            print "Message body: ",':'.join(x.encode('hex') for x in message_data)
            # Forwarding the message to the shim
            if identity not in connected_backends:
                connected_backends.append(identity)

            if decoded_header[NetIDEOps.NetIDE_header['TYPE']] is NetIDEOps.NetIDE_type['MODULE_ANNOUNCEMENT']:
                ack_message = NetIDEOps.netIDE_encode('MODULE_ACKNOWLEDGE', decoded_header[NetIDEOps.NetIDE_header['XID']], None, None, str(module_id))
                running_modules[message_data] = module_id
                module_id += 1
                backend.send_multipart([identity,ack_message])
            else:
                shim.send_multipart([shimname,message])

        if shim in sockets:
            # Get next client request, route to last-used worker
            (identity, message) = shim.recv_multipart()
            print "\n-----------Message from server controller ID ", identity
            decoded_header = NetIDEOps.netIDE_decode_header(message)
            print "Message header: ", decoded_header
            print "Message type: ", NetIDEOps.key_by_value(NetIDEOps.NetIDE_type,decoded_header[NetIDEOps.NetIDE_header['TYPE']])
            message_length = decoded_header[NetIDEOps.NetIDE_header['LENGTH']]
            message_data = message[NetIDEOps.NetIDE_Header_Size:NetIDEOps.NetIDE_Header_Size+message_length]
            print "Message body: ",':'.join(x.encode('hex') for x in message_data)
            # Forwarding the message to all the backends
            for backendname in connected_backends:
                print "Sending to backend: ", backendname
                backend.send_multipart([backendname,message])

    # Clean up
    backend.close()
    shim.close()
    context.term()

if __name__ == "__main__":
    main(sys.argv[1:])