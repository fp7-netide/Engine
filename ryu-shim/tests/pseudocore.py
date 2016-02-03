import zmq
import time

print('Binding PseudoCore...')
context = zmq.Context()
socket = context.socket(zmq.ROUTER)
socket.bind("tcp://*:5555")
print('Bound.')
while True:
    (identity, message) = socket.recv_multipart()
    print("Received message from '%s': %s" % (identity,message))
    time.sleep(1)
    print('Sending')

    #  Send reply back to client
    #socket.send(identity)
    #socket.send("")
    socket.send_multipart([identity, b"Hello World from PseudoCore. You sent: '" + message + "'"])