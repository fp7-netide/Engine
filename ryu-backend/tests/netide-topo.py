#!/usr/bin/python

from mininet.net import Mininet
from mininet.topo import Topo
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.node import RemoteController, OVSKernelSwitch

class SingleSwitchTopo(Topo):
    def __init__(self, **opts):
        # Initialize topology and default options
        Topo.__init__(self, **opts)
        
	#Firewall
        s11 = self.addSwitch( 's11', dpid = '000000000000000A', protocols = 'OpenFlow10')
        #s12 = self.addSwitch( 's12', dpid = '000000000000000B', protocols = 'OpenFlow10')
        #s13 = self.addSwitch( 's13', dpid = '000000000000000C')
	
	#Learning Switches
        s21 = self.addSwitch( 's21', dpid = '0000000000000001', protocols = 'OpenFlow10')
        s22 = self.addSwitch( 's22', dpid = '0000000000000002', protocols = 'OpenFlow10')
        s23 = self.addSwitch( 's23', dpid = '0000000000000003', protocols = 'OpenFlow10')
    		
	#Internal hosts
        alice = self.addHost( 'alice', ip='10.0.0.1', mac='0000000000E1')
        bob = self.addHost( 'bob', ip='10.0.0.2', mac='0000000000E2')
	
	#External host        
	charlie = self.addHost( 'charlie', ip='10.0.0.3', mac='0000000000E3')
	
	#Internal server
	www = self.addHost( 'www', ip='10.0.0.10', mac='0000000000EA')
               
        self.addLink(s11, charlie)
        self.addLink(s22, alice)
        self.addLink(s22, bob)
	self.addLink(s23, www)
        
        self.addLink(s11, s21)
        self.addLink(s22, s21)
        self.addLink(s23, s21)

        
        
def simpleTest():
    "Create and test a simple network"
    topo = SingleSwitchTopo()
    net = Mininet(topo, switch=OVSKernelSwitch, controller=RemoteController)
    c1 = 'tcp:192.168.56.1:6633'
    #net.staticArp()
    net.start()
    #print "Dumping host connections"
    s11, s21, s22, s23  = net.get('s11',  's21', 's22', 's23')
    s11.cmd('ovs-vsctl set-controller s11 %s', c1)
    s21.cmd('ovs-vsctl set-controller s21 %s', c1)
    s22.cmd('ovs-vsctl set-controller s22 %s', c1)
    s23.cmd('ovs-vsctl set-controller s23 %s', c1)
    CLI(net)
    net.stop()
    
    
 
if __name__ == '__main__':
    # Tell mininet to print useful information
    setLogLevel('info')
    simpleTest()
