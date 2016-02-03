import socket
import sys

s = socket.create_connection(("localhost", 41414))
print "Connected to shim server, sending test message"
s.send("## from test to shim server ##")
s.close()
print "Test done"