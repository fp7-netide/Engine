#!/bin/bash
#Creates the basic structure to later apply the updateEngine and runEngine scripts to launch the NetIDE Engine, also indicates what components to install (20/04/16)

NetIDE_DIR="$HOME/NetIDE"

#if [ "$1" == "-h" ] || [ "$1" == "--help" ]
#then
#	echo "Usage: setEngine.sh"
#	exit 0
#fi

#if [ -d $NetIDE_DIR ]; then
#        echo "NetIDE folder already exists"
#	exit 0
#fi

#mkdir $NetIDE_DIR
#cd $NetIDE_DIR
#git clone https://github.com/fp7-netide/Engine.git

echo -e "\nThis setup requires the following components:"
echo "  => git, curl,... (and other packages required by the Engine -> check GitHub)"
echo "  => Mininet >2.2.1"
echo "  => Maven >3.1 (specifically 3.3.3 for Java core + ODL shim; 3.3.9 for ONOS shim)"
echo "  => Java 8 Oracle (for ONOS shim)"
echo "  => Java 8 OpenJDK (for Java core + ODL shim)"
echo "  ===> sudo add-apt-repository ppa:openjdk-r/ppa"
echo "  ===> sudo apt-get update"
echo "  ===> sudo apt-get install openjdk-8-jdk"
echo "  ===> sudo update-alternatives --config java"
echo "  ===> sudo update-alternatives --config javac"

echo -e "\nAnd having installed inside NetIDE folder ($NetIDE_DIR):"
echo "  => Apache Karaf 3.0.5"
echo "  => Floodlight (v1.1)"
echo "  => ONOS (v1.4))"
echo "  => Ryu (v3.30)"
