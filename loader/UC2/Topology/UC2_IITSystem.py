#!/usr/bin/python
 
""" 
 Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html
 
 Authors:
	Elisa Rojas
"""


###############################################################################################
###        Name: UC2_IITSystem.py
###      Author: Elisa Rojas - elisa.rojas@telcaria.com
### Description: NetIDE UC2 Topology
###############################################################################################

from os import popen
import logging
import sys

# Mininet libraries
from mininet.cli  import CLI
from mininet.net  import Mininet
from mininet.topo import Topo
from mininet.util import errRun
from mininet.node import Controller, OVSKernelSwitch, RemoteController
from mininet.log import setLogLevel

# Configuration Files
IPCONFIG = './IPCONFIG.dat'

class IITSTopology (Topo):

	### Topology class members
	Devices = dict()

	### Topology constructor
	def __init__ (self, of_version):
		## Initialize Topology
		super(IITSTopology, self).__init__()

		logger = logging.getLogger('NetIDE Logger')

                #OpenFlow version
                if of_version == 'of13':
                    sconfig = {'protocols': 'OpenFlow13'}
                else:
                    sconfig = {'protocols': 'OpenFlow10'}
		# Switches
		SW1       = self.addSwitch('s1', **sconfig)
		SW2       = self.addSwitch('s2', **sconfig)
		SW3       = self.addSwitch('s3', **sconfig)
        	SW4       = self.addSwitch('s4', **sconfig)
    		SW1b      = self.addSwitch('s5', **sconfig)
		SW2b      = self.addSwitch('s6', **sconfig)
		SW3b      = self.addSwitch('s7', **sconfig)
        	SW4b      = self.addSwitch('s8', **sconfig)

		# Hosts
		H1        = self.addHost('h1')
		H2        = self.addHost('h2')
    		H3        = self.addHost('h3')
		H4        = self.addHost('h4')
		H5        = self.addHost('h5')

		# Hypervisors
		HH1        = self.addSwitch('s11', **sconfig)
		HH2        = self.addSwitch('s12', **sconfig)
                HH3        = self.addSwitch('s13', **sconfig)
		HH4        = self.addSwitch('s14', **sconfig)
                HH5	   = self.addSwitch('s15', **sconfig)

		# Add links
                #from hosts (H1,H2,H3,H4) to hosts-hypervisors (HH1,HH2,HH3,HH4)
       		#from down hosts-hypervisors (HH1,HH2,HH3,HH4) to SW1 and SW2
		self.addLink(H1,         HH1)
        	self.addLink(H3,         HH3)
		self.addLink(H2,         HH2)
        	self.addLink(H4,         HH4)
		self.addLink(H5,         HH5)
       		#from hosts-hypervisors (HH1,HH2,HH3,HH4) to SW1, SW2 and SW4
		self.addLink(HH1,         SW1)
		self.addLink(HH1,        SW1b)
        	self.addLink(HH3,         SW1)
		self.addLink(HH3,        SW1b)
        	self.addLink(HH2,         SW2)
		self.addLink(HH2,        SW2b)
        	self.addLink(HH4,         SW2)
		self.addLink(HH4,        SW2b)
                self.addLink(HH5,         SW4)
		self.addLink(HH5,        SW4b)
        	
                #from SW3 to SW1 and SW2
        	self.addLink(SW3,        SW1)
		self.addLink(SW3,       SW1b)
        	self.addLink(SW3,        SW2)
		self.addLink(SW3,       SW2b)
        	self.addLink(SW3b,       SW1)
		self.addLink(SW3b,      SW1b)
        	self.addLink(SW3b,       SW2)
		self.addLink(SW3b,      SW2b)
        	#from SW3 to SW4
        	self.addLink(SW3,        SW4)
		self.addLink(SW3,       SW4b)
        	self.addLink(SW3b,       SW4)
		self.addLink(SW3b,      SW4b)
		#between peers
		self.addLink(SW1,        SW2)
		self.addLink(SW1b,      SW2b)
		self.addLink(SW3,       SW3b)

		logger.info('NetIDE - UC2 Topology created')

	### Mutator for Devices
	def SetDevices(self, Devices):
		self.Devices = Devices

	### Assign IPs, Masks and Routes to devices
	def SetIPConfiguration(self, Devices, net):

		## Internal configuration for Mininet (so that pingall and other Mininet's commands work ok and use correct IPs and so)
		net.get("h1").setIP("10.0.1.11")		
		#net.get("h1").setIP("10.0.1.11", "24", "h1-eth0")
		#net.get("h1").setIP("10.1.1.11", "24", "h1-eth1")
                net.get("h2").setIP("10.0.1.12")
                #net.get("h2").setIP("10.0.1.12", "24", "h2-eth0")
		#net.get("h2").setIP("10.1.1.12", "24", "h2-eth1")
		net.get("h3").setIP("10.0.1.13", "24")
		#net.get("h3").setIP("10.0.1.13", "24", "h3-eth0")
		#net.get("h3").setIP("10.1.1.13", "24", "h3-eth1")
		net.get("h4").setIP("10.0.1.14", "24")
		#net.get("h4").setIP("10.0.1.14", "24", "h4-eth0")
		#net.get("h4").setIP("10.1.1.14", "24", "h4-eth1")
        	net.get("h5").setIP("10.0.1.15", "24")
		#net.get("h5").setIP("10.0.1.15", "24", "h5-eth0")
		#net.get("h5").setIP("10.1.1.15", "24", "h5-eth1")

		## Hosts configuration (ipconfig/ifconfig)
		try:
			with open(IPCONFIG, 'r') as f:
				for command in f:
					if( len(command.split()) == 0):
						break

					token = command.split('#')
					for host in Devices:
						if token[0] == host.name:
							for tok in token[1:len(token)]:
								tok.replace("\n", "")
								host.popen(tok) #popen() opens a process by creating a pipe, forking, and invoking the shell
							break

				logger.info('Successfully enforced initial ip settings to the UC topology')
				f.close()
				
		except EnvironmentError:
			logger.error("Couldn't load config file for ip addresses, check whether %s exists" % IPCONFIG)

### Create a logger and configure it
def ConfigureLogger():
	# Create a log instance
	logger = logging.getLogger('NetIDE Logger')

	# Set logging level
	logger.setLevel(logging.INFO)

	handler = logging.StreamHandler()
	handler.setLevel(logging.INFO)

	# Create a logging format
	formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
	handler.setFormatter(formatter)

	# Add the handlers to the logger
	logger.addHandler(handler)

	return logger

### Network Emulator method
def UC2_IITS_Network(of_version):

	logger.info('=================== Mininet Topology SetUp ===================')

	# Create UC Topology instance
	IITSTopo = IITSTopology(of_version)

	# Start mininet and load the topology
	net = Mininet( topo=IITSTopo, controller=RemoteController, autoSetMacs = True )
	net.start()
	setLogLevel( 'info' ) #setLogLevel( 'info' | 'debug' | 'output' )

	# Get the devices
	Devices = dict()
	Devices = net.get('h1', 'h2', 'h3', 'h4', 'h5', 's1', 's2', 's3', 's4', 's5', 's6', 's7', 's8')
	IITSTopo.SetDevices(Devices)
	logger.info('==============================================================\n')

	# Enforce IP configuration
	logger.info('====================== IP Configuration ======================')
	IITSTopo.SetIPConfiguration(Devices, net)
	logger.info('==============================================================\n')

	# Start mininet CLI
	CLI( net )

	# Destroy network after exiting the CLI
	net.stop()


#########################################################################################################
### Main
#########################################################################################################
if __name__ == '__main__':
        if (len(sys.argv) > 1):
           of_version = sys.argv[1]
        else:
           of_version = 'of10'
	# Setup the logger
	logger = ConfigureLogger()

	# Start network
	UC2_IITS_Network(of_version)
#########################################################################################################


#########################################################################################################
### Load this module from mininet externally
#########################################################################################################
logger = ConfigureLogger()
topos = { 'IITS': ( lambda: UC2_IITS_Network() ) }
#########################################################################################################
