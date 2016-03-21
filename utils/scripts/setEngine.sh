#!/bin/bash
#Creates the basic structure to later apply the runEngine and updateEngine scripts, also indicates what components to install

if [ -z "$1" ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]
then 
	echo "Usage: <setEngine.sh> <-i (Install)>"
	exit 0
fi

if [ "$1" == "-i" ]
then
	cd $HOME
	sudo mkdir NetIDE
	cd NetIDE
	git clone https://github.com/fp7-netide/Engine.git
	sudo mkdir Engine-mixed

	echo "This setup requires the following components:"
	echo "	  Packages: "
	echo "		git, curl......... (and other packages required by the Engine -> check GitHub)"
	echo "		Java 8: Oracle + JDK (specifically JDK for Java core + ODL shim; Oracle for ONOS shim)"
	echo "		Maven >3.1 (specifically 3.3.3 for Java core + ODL shim; 3.3.9 for ONOS shim)"
	echo "		Karaf 3.0.5 installed inside Engine-mixed"
	echo "    SDN controllers: Ryu, Floodlight v1.1, ONOS v1.4 (+create Downloads/Applications folders +compile)"
	echo "	  Mininet >2.2.1"   
fi
