OPENFLOW='OpenFlow13'

import os
from mininet.net import Mininet
from mininet.topo import Topo
from mininet.node import RemoteController
from mininet.cli import CLI

def myNet():
	
	net = Mininet(controller=RemoteController)
	net.addController( 'c0',ip='127.0.0.1')
 
        #NAT
        s1 = net.addSwitch( 's1', dpid = '000000000000000A', protocols = 'OpenFlow13')

        #Internal hosts
        alice = net.addHost( 'alice', ip='192.168.0.2/24', defaultRoute='via 192.168.0.1', mac='0000000000E1')
        bob = net.addHost( 'bob', ip='192.168.0.3/24', defaultRoute='via 192.168.0.1', mac='0000000000E2')

        #External host        
        charlie = net.addHost( 'charlie', ip='10.0.0.100', mac='0000000000E3')

        net.addLink(s1, alice)
        net.addLink(s1, bob)
        net.addLink(s1, charlie)
	net.start()

	alice.cmd('arp -s 192.168.0.1  00:00:00:00:00:e3 -i alice-eth0')
	alice.cmd('arp -s 192.168.0.3  00:00:00:00:00:e2 -i alice-eth0')
	bob.cmd('arp -s 192.168.0.1  00:00:00:00:00:e3 -i bob-eth0')
	bob.cmd('arp -s 192.168.0.2  00:00:00:00:00:e1 -i bob-eth0')
	charlie.cmd('arp -s 10.0.0.2  00:00:00:00:00:e1 -i charlie-eth0')
	charlie.cmd('arp -s 10.0.0.3  00:00:00:00:00:e2 -i charlie-eth0')
	
	c = net.get('c0')
    	CLI(net)

    	net.stop()

if __name__ == '__main__':
    myNet()
