"""Simple 3-layer tree topology with 8 hosts.

Pass as argument with '--custom=tree_8.py --topo=tree_8' from the command line.
"""

from mininet.topo import Topo

class Tree( Topo ):
    def __init__( self ):
        # Initialize topology
        Topo.__init__( self )

        # Add hosts and switches
        h1 = self.addHost('h1', mac="000000000001", ip="10.0.0.1")
        h2 = self.addHost('h2', mac="000000000002", ip="10.0.0.2")
        h3 = self.addHost('h3', mac="000000000003", ip="10.0.0.3")
        h4 = self.addHost('h4', mac="000000000004", ip="10.0.0.4")
        h5 = self.addHost('h5', mac="000000000005", ip="10.0.0.5")
        h6 = self.addHost('h6', mac="000000000006", ip="10.0.0.6")
        h7 = self.addHost('h7', mac="000000000007", ip="10.0.0.7")
        h8 = self.addHost('h8', mac="000000000008", ip="10.0.0.8")
        s1 = self.addSwitch('s1', dpid="0000000000000001")
        s2 = self.addSwitch('s2', dpid="0000000000000002")
        s3 = self.addSwitch('s3', dpid="0000000000000003")
        s4 = self.addSwitch('s4', dpid="0000000000000004")
        s5 = self.addSwitch('s5', dpid="0000000000000005")
        s6 = self.addSwitch('s6', dpid="0000000000000006")
        s7 = self.addSwitch('s7', dpid="0000000000000007")

        # Add links
        self.addLink(h1, s1)
        self.addLink(h2, s1)
        self.addLink(h3, s2)
        self.addLink(h4, s2)
        self.addLink(h5, s3)
        self.addLink(h6, s3)
        self.addLink(h7, s4)
        self.addLink(h8, s4)

        self.addLink(s1, s5)
        self.addLink(s2, s5)
        self.addLink(s3, s6)
        self.addLink(s4, s6)

        self.addLink(s5, s7)
        self.addLink(s6, s7)

topos = {'tree_8': (lambda: Tree())}
