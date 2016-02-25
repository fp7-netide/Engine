from mininet.topo import Topo

class MyTopo( Topo ):
    "Simple topology example."

    def __init__( self ):
        "Create custom topo."

        # Initialize topology
        Topo.__init__( self )

        #F irewall
        s11 = self.addSwitch( 's11', dpid = '000000000000000A', protocols = 'OpenFlow13')
        #s12 = self.addSwitch( 's12', dpid = '000000000000000B', protocols = 'OpenFlow10')
        #s13 = self.addSwitch( 's13', dpid = '000000000000000C')

        #Learning Switches
        s21 = self.addSwitch( 's21', dpid = '0000000000000001', protocols = 'OpenFlow13')
        s22 = self.addSwitch( 's22', dpid = '0000000000000002', protocols = 'OpenFlow13')
        s23 = self.addSwitch( 's23', dpid = '0000000000000003', protocols = 'OpenFlow13')

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


topos = { 'mytopo': ( lambda: MyTopo() ) }
