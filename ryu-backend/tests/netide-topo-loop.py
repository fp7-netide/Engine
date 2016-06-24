from mininet.topo import Topo

class MyTopo( Topo ):
    "Topology compose by 3 switches creating a loop."

    def __init__( self ):
        "Create custom topo."

        # Initialize topology
        Topo.__init__( self )

        #Learning Switches
        s1 = self.addSwitch( 's1', dpid = '0000000000000001')
        s2 = self.addSwitch( 's2', dpid = '0000000000000002')
        s3 = self.addSwitch( 's3', dpid = '0000000000000003')

        #Hosts
        alice = self.addHost( 'alice', ip='10.0.0.1', mac='0000000000E1')
        bob = self.addHost( 'bob', ip='10.0.0.2', mac='0000000000E2')

        self.addLink(s1, alice)
        self.addLink(s3, bob)

        self.addLink(s1, s2)
        self.addLink(s2, s3)
        self.addLink(s3, s1)


topos = { 'mytopo': ( lambda: MyTopo() ) }