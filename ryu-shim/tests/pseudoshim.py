import zmq
import time
import threading
import sys

class CoreConnection(threading.Thread):
    def __init__(self, port, id):
    	threading.Thread.__init__(self)
        self.port = port
        self.id = id

    def run(self):
        print("Connecting to Core at port " + self.port + " as '" + self.id + "'")
        context = zmq.Context()
        self.socket = context.socket(zmq.DEALER)
        self.socket.setsockopt(zmq.IDENTITY, self.id)
        self.socket.connect("tcp://localhost:" + self.port)
        print('Connected.')
        #self.socket.send(b"First Hello from " + self.id)
        while True:
            message = self.socket.recv_multipart()
            print("Received message: %s" % message[0])

if len(sys.argv) < 3:
	print('You have to specify the port and then the id for the pseudoshim as arguments!')
	sys.exit()
conn = CoreConnection(sys.argv[1], sys.argv[2])
conn.Daemon = True
conn.start()
try:
    while True:   
        msg = raw_input('-->')
        conn.socket.send(msg)
except KeyboardInterrupt:
    conn._Thread__stop()
    sys.exit()