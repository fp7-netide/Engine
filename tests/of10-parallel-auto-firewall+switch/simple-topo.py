from mininet.topo import Topo

class MyTopo( Topo ):

    def __init__( self ):

        Topo.__init__( self )

	s1 = self.addSwitch( 's1', dpid = '0000000000000001')
        alice = self.addHost( 'alice', ip='10.0.0.1', mac='0000000000E1')
        bob = self.addHost( 'bob', ip='10.0.0.2', mac='0000000000E2')

	self.addLink(s1,alice)
	self.addLink(s1,bob)

topos = { 'mytopo': ( lambda: MyTopo() ) }
