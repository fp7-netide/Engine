#!/bin/bash
#Runs the different parts of the Engine: 1 call = 1 item in the architecture (23/02/16)

ERROR=-1
echo ""
echo "========================================= NetIDE Engine =========================================="
# Start backend (-b), core (-c), shim (-s) or help (-h/--help)
if [ -z "$1" ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]
then
	echo "Usage: <runEngine.sh> <-b (backend) / -c (core) / -s (shim) / -m (Mininet)> <type (Ryu/Floodlight; Java/Python; Ryu/ODL/ONOS; netide-topo)>"
	exit 0
fi

#Go to Engine folder
cd $HOME/NetIDE/Engine-mixed
ls

#Start backend
if [ "$1" == "-b" ]
then 
	echo "---- Backend:" $2 "----"
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
			#sudo ovs-ofctl -O Openflow13 add-flow s11 actions=CONTROLLER
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
	echo "---- Shim:" $2 "----"
	if [ "$2" == "Ryu" ] || [ "$2" == "ryu" ]
	then
		cd ryu-shim
		ryu-manager ryu-shim.py
	elif [ "$2" == "ODL" ] || [ "$2" == "odl" ]
	then
		export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
		export MAVEN_OPTS='-Xmx1048m -XX:MaxPermSize=512m'
		echo "odl> feature:install odl-netide-rest"
		echo "log:display | grep 'NetideProvider Session Initiated'"
		cd odl-shim/karaf/target/assembly/bin
		./karaf
	elif [ "$2" == "ONOS" ] || [ "$2" == "onos" ]
	then
		export ONOS_ROOT=$HOME/NetIDE/onos
		source $ONOS_ROOT/tools/dev/bash_profile
		cell netide
		echo "---To modify core address - default is localhost:5555 -, run the following:"
		echo "onos> config:edit eu.netide.shim.ShimLayer"
		echo "onos> property-set coreAddress *CORE IP*"
		echo "onos> property-set corePort *CORE PORT*"
		echo "onos> config:update"
		echo "---To start the shim, run the following command in the odl-shim folder"
		echo "onos-app $OC1 install target/onos-app-shim-1.0.0-SNAPSHOT.oar"
                onos-karaf clean
	else
		echo "Error! Shim" $2 "not available... <choose type (Ryu/ODL/ONOS)>"
	fi

#Start core
elif [ "$1" == "-c" ]
then
	echo "---- Core:" $2 "----"
	if [ "$2" == "Java" ] || [ "$2" == "java" ]
	then
		export JAVA_HOME=/usr/lib/jvm/java-8-oracle/
		echo "karaf> feature:repo-add mvn:eu.netide.core/core.features/1.1.0-SNAPSHOT/xml/features (1.0.0.0) (1.0.1.0)"
		echo "karaf> feature:install core (netide-core)"
		echo "karaf> netide:loadcomposition /path/to/composition (DpidPartitionABCW.xml)"
		echo "karaf> netide:listmodules"
		#cd apache-karaf-3.0.3/
		cd apache-karaf-3.0.5/
		bin/karaf clean --verbose
	elif [ "$2" == "Python" ] || [ "$2" == "python" ] 
	then
		if [ -z "$3" ]
		then
			COMPFILE="CompositionSpecification.xml" #default composition file
		else
			COMPFILE=$3
		fi
		cd ryu-backend/tests
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
	echo "---- Mininet:" $2:$3"(protocol)":$4"(port) ----"
	if [ "$2" == "netide-topo" ] || [ "$2" == "netide" ] || [ -z "$2" ]
	then
		if [ -z "$4" ]
		then
			PORT=6633 #default port for Mininet
		else
			PORT=$4
		fi
		cd ryu-backend/tests
		if [ "$3" == "of10" ] || [ -z "$3" ]
		then
			sudo mn --custom netide-topo.py --topo mytopo --controller=remote,ip=127.0.0.1,port=$PORT
		elif [ "$3" == "of13" ]
		then
			sudo mn --custom netide-topo.py --topo mytopo --controller=remote,ip=127.0.0.1,port=$PORT --switch ovsk,protocols=OpenFlow13
		else
			echo "Error! Not such option" $3 "available... <choose type (of10,of13,etc.)>"
		fi
	else
		echo "Error! Mininet test" $2 "not available... <choose type (netide-topo)>"
	fi
fi
echo "=================================================================================================="
echo ""
