#!/bin/bash
#Runs the different parts of the Engine: 1 call = 1 item in the architecture

NetIDE_DIR="$HOME/NetIDE"
EngineMixed_DIR="$NetIDE_DIR/Engine-mixed"

# Start backend (-b), core (-c), shim (-s) or help (-h/--help)
if [ -z "$1" ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]
then
        echo "Usage: runEngine.sh -b (backend) Ryu/Floodlight"
	echo "                    -c (core) Java/Python"
	echo "                    -s (shim) Ryu/ODL/ONOS"
        echo "                    -m (Mininet) netide-topo of1X [6644]"
	exit 0
fi

echo -e "\n==========================="
echo "====== NetIDE Engine ======"
echo "==========================="

#Go to the Engine folder
cd $EngineMixed_DIR

#Start backend
if [ "$1" == "-b" ]
then
	echo -e "=> Backend: $2\n"
	if [ "$2" == "Ryu" ] || [ "$2" == "ryu" ]
	then
		cd ryu-backend
		if [ "$3" == "of10" ] || [ "$3" == "of10-sw+fw" ]
		then
			if [ "$4" == "-p" ]
			then
				cd $HOME/NetIDE/
				python -m cProfile -o statistics ryu/bin/ryu-manager --ofp-tcp-listen-port 7733 Engine-mixed/ryu-backend/ryu-backend.py Engine-mixed/ryu-backend/tests/simple_switch.py Engine-mixed/ryu-backend/tests/firewall.py
			else
				ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py tests/firewall.py
			fi
		elif [ "$3" == "of10-sw" ]
		then
			ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py
		elif [ "$3" == "of10-fw" ]
		then
			ryu-manager --ofp-tcp-listen-port 7734 ryu-backend.py tests/firewall.py
		elif [ "$3" == "of13" ] || [ "$3" == "of13-sw+fw" ]
		then
			if [ "$4" == "-p" ]
			then
				cd $HOME/NetIDE/
				python -m cProfile -o statistics ryu/bin/ryu-manager --ofp-tcp-listen-port 7733 Engine-mixed/ryu-backend/ryu-backend.py Engine-mixed/ryu-backend/tests/simple_switch_13.py Engine-mixed/ryu-backend/tests/firewall_13.py
			else
				ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch_13.py tests/firewall_13.py
			fi
		elif [ "$3" == "of13-sw" ]
		then
			ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch_13.py
		elif [ "$3" == "of13-fw" ]
		then
			ryu-manager --ofp-tcp-listen-port 7734 ryu-backend.py tests/firewall_13.py
		else
			echo "Error! Not such option" $3 "available... <choose type (of10,of13,etc.)>"
		fi
	elif [ "$2" == "Floodlight" ] || [ "$2" == "floodlight" ] || [ "$2" == "flo" ]
	then
		cd $HOME/NetIDE/floodlight
		./floodlight.sh
	else
		echo "Error! Backend" $2 "not available... <choose type (Ryu/Floodlight)>"
	fi

#Start shim
elif [ "$1" == "-s" ]
then
	echo "=> Shim: $2"
	if [ "$2" == "Ryu" ] || [ "$2" == "ryu" ]
	then
		cd ryu-shim
		ryu-manager ryu-shim.py
	elif [ "$2" == "ODL" ] || [ "$2" == "odl" ]
	then
		export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
		export MAVEN_OPTS='-Xmx1048m -XX:MaxPermSize=512m'
                echo -e "\n> To install NetIDE shim:"
                echo -e "feature:install odl-netide-rest\n"
                echo -e "> To check the installation::"
                echo -e "log:display | grep 'NetideProvider Session Initiated'\n"
		cd odl-shim/karaf/target/assembly/bin
		./karaf
	elif [ "$2" == "ONOS" ] || [ "$2" == "onos" ]
	then
		export ONOS_ROOT=$HOME/NetIDE/onos
		source $ONOS_ROOT/tools/dev/bash_profile
		cell netide
		echo -e "\n> To modify core address (by default: localhost:5555), run the following command:"
		echo "config:edit eu.netide.shim.ShimLayer"
		echo "property-set coreAddress *CORE IP*"
		echo "property-set corePort *CORE PORT*"
		echo "config:update"
                echo -e "\n> To permit extraneous network rules:"
                echo "onos:cfg set org.onosproject.net.flow.impl.FlowRuleManager allowExtraneousRules true"
		echo -e "\n> To install the shim, run the following command in the onos-shim folder:"
		echo "onos-app $OC1 install target/onos-app-shim-1.0.0-SNAPSHOT.oar"
                echo -e "\n> To activate the shim:"
                echo -e "app activate eu.netide.shim\n"
                onos-karaf clean
	else
		echo "Error! Shim" $2 "not available... <choose type (Ryu/ODL/ONOS)>"
	fi

#Start core
elif [ "$1" == "-c" ]
then
	echo "=> Core: $2"
	if [ "$2" == "Java" ] || [ "$2" == "java" ]
	then
		cd $NetIDE_DIR/apache-karaf-*
		bin/start clean
		echo -e "\nStarting Karaf..."
		sleep 15
		bin/client feature:repo-add mvn:eu.netide.core/core.features/1.1.0-SNAPSHOT/xml/features
		bin/client feature:install core
		bin/client netide:loadcomposition DpidPartitionABCW.xml
		sleep 2
		echo -e "\n> To list NetIDE modules:"
		echo -e "netide:listmodules\n"
		echo -e "> To shutdown Karaf:"
		echo -e "shutdown -f\n"
		bin/client
	elif [ "$2" == "Python" ] || [ "$2" == "python" ]
	then
		if [ -z "$3" ]
		then
			COMPFILE="CompositionSpecification.xml" #default composition file
		else
			COMPFILE=$3
		fi
		cd $NetIDE_DIR/Python-Core
		echo "Loading Python core with" $COMPFILE
		cat $COMPFILE
		echo ""
		python AdvancedProxyCore.py -c $COMPFILE
	else
		echo "Error! Core" $2 "not available... <choose type (Java/Python)>"
	fi

#Start Mininet topology
elif [ "$1" == "-m" ]
then
	if [ "$2" == "netide-topo" ] || [ "$2" == "netide" ] || [ -z "$2" ]
	then
                if [ -z "$3" ]
                then
                        OF_VERSION=OpenFlow10 #default OpenFlow Version for Mininet
                else
                        OF_VERSION=$3
                fi
		if [ -z "$4" ]
		then
			PORT=6633 #default port for Mininet
		else
			PORT=$4
		fi
	        echo "--- Mininet $2 => Protocol: $OF_VERSION , Port: $PORT ---"
		cd ryu-backend/tests
		sudo mn --custom netide-topo.py --topo mytopo --controller=remote,ip=127.0.0.1,port=$PORT --switch ovsk,protocols=$OF_VERSION
	else
		echo "Error! Mininet test" $2 "not available... <choose type (netide-topo)>"
	fi
fi

echo -e "\n==========================="
