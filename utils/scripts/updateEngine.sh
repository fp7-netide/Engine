#!/bin/bash
#Updates Engine code merging the working branches/tags in a single Engine-mixed folder

#NOTES:
#       ODL compiles with JDK8
#       ONOS and Java core compile with Java8 (Oracle)
#       This requires changing JAVA_HOME => sudo update-alternatives --config java
#	Ryu v3.30 (git checkout v3.30)
#	Set Maven: export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.3
#	           $M2_HOME/conf/settings.xml => <localRepository>/home/netide/.m2/repository</localRepository>
#	In the Maven repository, jar.lastUpdated indicates the jar was not found and Maven tried to find it online

NetIDE_DIR="$HOME/NetIDE"
Engine_DIR="$NetIDE_DIR/Engine"
EngineMixed_DIR="$NetIDE_DIR/Engine-mixed"

if [ -z "$1" ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]
then
	echo "Usage: updateEngine.sh -j (Java core)"
	echo "                       -r (Ryu backend/shim)"
	echo "                       -o (ODL shim)"
        echo "                       -n (ONOS shim)"
        echo "                       -f (Floodlight backend)"
        echo "                       -a (ALL)"
	exit 0
fi

#cd $NetIDE_DIR/Engine
#git reset --hard #completely remove all staged and unstaged changes to tracked files
#git clean        #remove files that are not tracked
#git checkout .   #undo all changes in my working tree

# Load bashrc
PS1='$ ' . ~/.bashrc

echo -e "\n==========================="
echo "====== NetIDE Engine ======"
echo "==========================="

#Go to the NetIDE folder
cd $NetIDE_DIR
if [ ! -d "Engine-mixed" ]; then
	echo -e "\nCreating Engine-mixed folder..."
	mkdir Engine-mixed
fi

echo -e "\nUpdating Engine..."
cd $Engine_DIR
git checkout master
git pull

#Ryu backend/shim
if [[ "$1" == *"r"* ]] || [ "$1" == "-a" ]
then
        echo -e "\n==========================="
        echo "=> Ryu backend/shim"

        echo -e "\n=> Deleting current state in Engine-mixed"
        echo "Previous folder:"
        ls -alh $EngineMixed_DIR
	sudo rm -rf $EngineMixed_DIR/ryu*
	sudo rm -rf $EngineMixed_DIR/libraries
        sleep 2

        echo -e "\n=> Updating Ryu backend/shim"
	cd $Engine_DIR
	cp -r ryu* $EngineMixed_DIR
	cp -r libraries $EngineMixed_DIR
        echo "Current folder:"
        ls -alh $EngineMixed_DIR
        sleep 5

        echo -e "\n=> Installing Ryu backend/shim"
	sudo rm -rf $NetIDE_DIR/ryu/ryu/netide
	cp -r $EngineMixed_DIR/libraries/netip/python $NetIDE_DIR/ryu/ryu/netide
	cd $NetIDE_DIR/ryu
	sudo python ./setup.py install
	sleep 5
fi

#Java core
if [[ "$1" == *"j"* ]] || [ "$1" == "-a" ]
then
	echo -e "\n==========================="
	echo "=> Java core"

	echo -e "\n=> Deleting current state in Engine-mixed"
	echo "Previous folder:"
	ls -alh $EngineMixed_DIR
	sudo rm -rf $EngineMixed_DIR/core
	sudo rm -rf $EngineMixed_DIR/libraries
	sleep 2

	echo -e "\n=> Updating Java core"
        cd $Engine_DIR
	cp -r core $EngineMixed_DIR
	cp -r libraries $EngineMixed_DIR
	echo "Current folder:"
	ls -alh $EngineMixed_DIR
	sleep 5

	echo -e "\n=> Installing Java core"
	cd $EngineMixed_DIR/core
	mvn clean install -Dgpg.skip=true -DskipTests
	cp core.branding/target/core.branding-1.1.0-SNAPSHOT.jar $NetIDE_DIR/apache-karaf-3.0.5/lib
	cp specification/DpidPartitionABCW.xml $NetIDE_DIR/apache-karaf-3.0.5
	sleep 5
fi

#ODL shim
if [[ "$1" == *"o"* ]] || [ "$1" == "-a" ]
then
        echo -e "\n==========================="
        echo "=> ODL shim"

        echo -e "\n=> Deleting current state in Engine-mixed"
        echo "Previous folder:"
        ls -alh $EngineMixed_DIR
        sudo rm -rf $EngineMixed_DIR/odl-shim
        sleep 2

        echo -e "\n=> Updating ODL shim"
        #cd $Engine_DIR
	#cp -r odl-shim $EngineMixed_DIR
	cd $EngineMixed_DIR
	git clone https://github.com/opendaylight/netide.git odl-shim
	cd odl-shim
	git checkout stable/beryllium
        echo "Current folder:"
        ls -alh $EngineMixed_DIR
        sleep 5

        echo -e "\n=> Installing ODL shim"
	mkdir -p ~/.m2
	wget -q -O - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > ~/.m2/settings.xml
	export MAVEN_OPTS='-Xmx1048m -XX:MaxPermSize=512m'
	mvn clean install -DskipTests
	sleep 5

        echo -e "\n=> Changing ports in ODL shim (1099->1098 and 44444->44445)"
	sed -i 's/1099/1098 #1099/g' karaf/target/assembly/etc/org.apache.karaf.management.cfg
        sed -i 's/44444/44445 #44444/g' karaf/target/assembly/etc/org.apache.karaf.management.cfg
	sleep 5
fi

#ONOS shim
if [[ "$1" == *"n"* ]] || [ "$1" == "-a" ]
then
        echo -e "\n==========================="
        echo "=> ONOS shim"

        echo -e "\n=> Deleting current state in Engine-mixed"
        echo "Previous folder:"
        ls -alh $EngineMixed_DIR
        sudo rm -rf $EngineMixed_DIR/onos-shim
        sleep 2

        echo -e "\n=> Updating ONOS shim"
        cd $Engine_DIR
	cp -r onos-shim $EngineMixed_DIR
        echo "Current folder:"
        ls -alh $EngineMixed_DIR
        sleep 5

        echo -e "\n=> Installing ONOS shim"
	cd $EngineMixed_DIR/onos-shim
	mvn clean install
	cd $NetIDE_DIR/onos
	mvn clean install -DskipTests #Compilation for 'onos-branding'
	sleep 5

        echo -e "\n=> Creating NetIDE cell"
	if [ -f $NetIDE_DIR/onos/tools/test/cells/netide ]
	then
		echo "Saving old NetIDE cell as netide.old"
		mv $NetIDE_DIR/onos/tools/test/cells/netide $NetIDE_DIR/onos/tools/test/cells/netide.old
	fi
	touch $NetIDE_DIR/onos/tools/test/cells/netide
	sh -c 'echo "# NetIDE cell

# the address of the VM to install the package onto
export OC1=\"192.168.56.101\"

# the default address used by ONOS utilities when none are supplied
export OCI=\"192.168.56.101\"

# the ONOS apps to load at startup
export ONOS_APPS=\"drivers,openflow,proxyarp,mobility\"

# the Mininet VM (if you have one)
#export OCN=\"192.168.56.102\"

# pattern to specify which address to use for inter-ONOS node communication (not used with single-instance $
export ONOS_NIC=\"192.168.56.*\"

export ONOS_USER=karaf
export ONOS_GROUP=karaf
export ONOS_WEB_USER=karaf
export ONOS_WEB_PASS=karaf" > '$NetIDE_DIR'/onos/tools/test/cells/netide'
fi

#Floodlight backend
if [[ "$1" == *"f"* ]] || [ "$1" == "-a" ]
then
        echo -e "\n==========================="
        echo "=> Floodlight backend"

        echo -e "\n=> Deleting current state in Engine-mixed"
        echo "Previous folder:"
        ls -alh $EngineMixed_DIR
	sudo rm -rf $EngineMixed_DIR/floodlight-backend
	sleep 2

        echo -e "\n=> Updating Floodlight Backend"
        cd $Engine_DIR
	cp -r floodlight-backend $EngineMixed_DIR
        echo "Current folder:"
        ls -alh $EngineMixed_DIR
        sleep 5

        echo -e "\n=> Installing Floodlight backend (v1.2)"
	cd $EngineMixed_DIR/floodlight-backend/v1.2
	cp -r src/main/java/net/floodlightcontroller/interceptor $NetIDE_DIR/floodlight/src/main/java/net/floodlightcontroller/
	cp lib/j* $NetIDE_DIR/floodlight/lib/
	cp lib/netip* $NetIDE_DIR/floodlight/lib/
	cp src/main/resources/floodlightdefault.properties $NetIDE_DIR/floodlight/src/main/resources/floodlightdefault.properties
	cd $NetIDE_DIR/floodlight
	sed -i '/<patternset id="lib">/a <include name="netip-1.1.0-SNAPSHOT.jar"/>' build.xml
	sed -i '/<patternset id="lib">/a <include name="javatuples-1.2.jar"/>' build.xml
	sed -i '/<patternset id="lib">/a <include name="jeromq-0.3.4.jar"/>' build.xml
	sed -i '$ a net.floodlightcontroller.interceptor.NetIdeModule' src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
	sed -i 's/6653/7753/' src/main/java/net/floodlightcontroller/core/internal/Controller.java
	sed -i 's/JVM_OPTS="\$JVM_OPTS -XX:CompileThreshold=1500 -XX:PreBlockSpin=8"/JVM_OPTS="\$JVM_OPTS -XX:CompileThreshold=1500"/' floodlight.sh
	sed -i '/FL_HOME=.*/i cd \$(dirname \$0)' floodlight.sh
	sed -i 's/FL_HOME=.*/FL_HOME=\./' floodlight.sh
	make
	sleep 5
fi

echo -e "\n==========================="
