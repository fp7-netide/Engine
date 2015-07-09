import zmq
import time
import threading
import sys

class CoreConnection(threading.Thread):
    def __init__(self, id):
    	threading.Thread.__init__(self)
        self.id = id

    def run(self):
        print('Connecting to Core...')
        context = zmq.Context()
        self.socket = context.socket(zmq.DEALER)
        self.socket.setsockopt(zmq.IDENTITY, self.id)
        self.socket.connect("tcp://localhost:5555")
        print('Connected.')
        self.socket.send(b"First Hello from " + self.id)
        while True:
            message = self.socket.recv_multipart()
            print("Received message: %s" % message[0])

conn = CoreConnection(sys.argv[1])
conn.Daemon = True
conn.start()
try:
    while True:   
        msg = raw_input('-->')
        conn.socket.send(msg)
except KeyboardInterrupt:
    conn._Thread__stop()
    exit