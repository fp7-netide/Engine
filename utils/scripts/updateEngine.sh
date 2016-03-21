#!/bin/bash
#Updates Engine code merging the working branches/tags in a single Engine-mixed folder (23/02/16)

if [ -z "$1" ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]
then 
	echo "Usage: <updateEngine.sh> <-r (Ryu backend/shim + Python core) / -j (Java core) / -o (ODL shim) / -n (ONOS shim) / -f (Floodlight backend) / -a (ALL)>"
	exit 0
fi

cd $HOME/NetIDE/Engine
git reset --hard #completely remove all staged and unstaged changes to tracked files
#git clean #remove files that are not tracked
#git checkout . #undo all changes in my working tree

#NOTES:
#	ODL compiles with JDK8
#	ONOS and Java core compile with Java8 (Oracle)
#	This requires changing JAVA_HOME + sudo update-alternative --config java(c)
#	#export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.3 #conf/settings.xml -> <localRepository>/home/netide/.m2/repository</localRepository> #jar.lastUpdated indicates the jar was not found and maven tried to find it online (it's a cache, to not being trying every time)

#Ryu backend/shim + Python core
if [[ "$1" == *"r"* ]] || [ "$1" == "-a" ]
then
	echo "===== Ryu backend/shim + Python core ====="
	echo "---Delete current state in Engine-mixed"
	ls $HOME/NetIDE/Engine-mixed
	sudo rm -rf $HOME/NetIDE/Engine-mixed/librariesPython
	sudo rm -rf $HOME/NetIDE/Engine-mixed/ryu*
	ls $HOME/NetIDE/Engine-mixed
	sleep 2

	echo "---Update Python core + Ryu backend/shim (master)"
	git fetch
	git checkout master #ryu-protocol-v1.1
	git pull origin master
	sudo cp -r ryu* $HOME/NetIDE/Engine-mixed/
	sudo cp -r libraries $HOME/NetIDE/Engine-mixed/librariesPython
	ls $HOME/NetIDE/Engine-mixed/
	sleep 5

	echo "---Install Python core + Ryu backend/shim"
	cd $HOME/NetIDE/Engine-mixed/
	sudo cp -r librariesPython/netip/python $HOME/NetIDE/ryu/ryu
	cd $HOME/NetIDE/ryu/ryu
	sudo rm -rf netide
	sudo mv python netide
	cd ..
	sudo python ./setup.py install
	sleep 5

	echo "---Final checks..."
	cd $HOME/NetIDE/Engine/floodlight-backend/v1.2/test
	cp FLCompositionSpecification.xml $HOME/NetIDE/Engine-mixed/ryu-backend/tests/	#copy to Python core
fi

#Java core
if [[ "$1" == *"j"* ]] || [ "$1" == "-a" ]
then
	echo "===== Java core =========================="
	echo "---Delete current state in Engine-mixed"
	ls $HOME/NetIDE/Engine-mixed
	sudo rm -rf $HOME/NetIDE/Engine-mixed/core
	sudo rm -rf $HOME/NetIDE/Engine-mixed/librariesJava
	ls $HOME/NetIDE/Engine-mixed
	sleep 2

	echo "---Update Java core (tags/demo-brussels)"
	git fetch
	#git checkout CoreImplementation
	#git pull origin CoreImplementation
	#git checkout tags/demo-brussels
	git checkout master
	sudo cp -r core $HOME/NetIDE/Engine-mixed/
	sudo cp -r libraries $HOME/NetIDE/Engine-mixed/libraries #Java
	ls $HOME/NetIDE/Engine-mixed/
	sleep 5

	echo "---Install Java core"
	sudo chmod +777 -R $HOME/NetIDE/Engine-mixed
	#sudo export JAVA_HOME=/usr/lib/jvm/java-8-oracle/
	cd $HOME/NetIDE/Engine-mixed/
	cd librariesJava/netip/java
	sudo mvn clean install -Dgpg.passphrase=netide #-Dgpg.skip=true 
	cd $HOME/NetIDE/Engine-mixed/core
	sudo mvn clean install -Dgpg.passphrase=netide
	cd tools/emulator
	sudo mvn package 
	sudo mv ../MinimalSpecification.xml target/
	cd ../..
	sudo mvn clean install -Dgpg.passphrase=netide
	sleep 5
fi

#ODL shim
if [[ "$1" == *"o"* ]] || [ "$1" == "-a" ]
then
	echo "===== ODL shim ==========================="
	echo "---Delete current state in Engine-mixed"
	ls $HOME/NetIDE/Engine-mixed
	sudo rm -rf $HOME/NetIDE/Engine-mixed/odl-shim
	ls $HOME/NetIDE/Engine-mixed
	sleep 2

	echo "---Update ODL shim (master)"
	git fetch
	git checkout master 
	git pull origin master
	#sudo cp -r odl-shim $HOME/NetIDE/Engine-mixed/
	cd $HOME/NetIDE/Engine-mixed/
	git clone https://github.com/opendaylight/netide.git
	sudo mv netide odl-shim
	ls $HOME/NetIDE/Engine-mixed/
	sleep 5

	echo "---Install ODL shim"
	sudo chmod +777 -R $HOME/NetIDE/Engine-mixed #to allow mvn clean install without 'sudo'
	#sudo export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
	sudo export MAVEN_OPTS='-Xmx1048m -XX:MaxPermSize=512m'
	cd $HOME/NetIDE/Engine-mixed/odl-shim
	mvn clean install
	sleep 5

	echo "---Final checks..."
	sudo cp $HOME/NetIDE/org.apache.karaf.management.cfg $HOME/NetIDE/Engine-mixed/odl-shim/karaf/target/assembly/etc/org.apache.karaf.management.cfg #Changing ports (1099->1098 and 44444->44445)	
fi

#ONOS shim
if [[ "$1" == *"n"* ]] || [ "$1" == "-a" ]
then
	echo "===== ONOS shim =========================="
	echo "---Delete current state in Engine-mixed"
	ls $HOME/NetIDE/Engine-mixed
	sudo rm -rf $HOME/NetIDE/Engine-mixed/onos-shim
	ls $HOME/NetIDE/Engine-mixed
	sleep 2

	echo "---Update ONOS shim (master)"
	git fetch
	git checkout master 
	git pull origin master
	sudo cp -r onos-shim $HOME/NetIDE/Engine-mixed/
	ls $HOME/NetIDE/Engine-mixed/
	sleep 5

	echo "---Install ONOS shim"
	sudo chmod +777 -R $HOME/NetIDE/Engine-mixed #to allow mvn clean install without 'sudo'
	cd $HOME/NetIDE
	cp netide_cell onos/tools/test/cells/netide
	cd onos
	cell netide
	cd $HOME/NetIDE/Engine-mixed/onos-shim
	mvn clean install
	cd $HOME/NetIDE/onos
	mvn clean install #I had to compile the project twice for 'onos-branding', etc to appear (don't know if it's always necessary...)
	sleep 5

	#echo "---Final checks..."
	#sudo cp $HOME/NetIDE/org.apache.karaf.management.cfg $HOME/NetIDE/Engine-mixed/odl-shim/karaf/target/assembly/etc/org.apache.karaf.management.cfg #Changing ports (1099->1098 and 44444->44445)	
fi

#FL backend
if [[ "$1" == *"f"* ]] || [ "$1" == "-a" ]
then
	echo "===== FL backend ========================="
	echo "---Delete current state in Engine-mixed"
	ls $HOME/NetIDE/Engine-mixed
	sudo rm -rf $HOME/NetIDE/Engine-mixed/floodlight-backend
	ls $HOME/NetIDE/Engine-mixed
	sleep 2
	#Add update of floodlight folder v1.1? (since it is modified later on)

	echo "---Update FL backend v1.2 (master)"
	git fetch
	git checkout master 
	git pull origin master
	sudo cp -r floodlight-backend $HOME/NetIDE/Engine-mixed/
	ls $HOME/NetIDE/Engine-mixed/
	sleep 5

	echo "---Install FL backend v1.2"
	sudo chmod +777 -R $HOME/NetIDE/Engine-mixed
	cd $HOME/NetIDE/Engine-mixed/floodlight-backend/v1.2
	cp -r src/main/java/net/floodlightcontroller/interceptor $HOME/NetIDE/floodlight/src/main/java/net/floodlightcontroller/
	cp lib/j* $HOME/NetIDE/floodlight/lib/
	cp lib/netip* $HOME/NetIDE/floodlight/lib/
	cp src/main/resources/floodlightdefault.properties $HOME/NetIDE/floodlight/src/main/resources/floodlightdefault.properties
	cd $HOME/NetIDE
	cp build.xml floodlight/build.xml #to be updated by Giuseppe (for the IDE)
	cp net.floodlightcontroller.core.module.IFloodlightModule floodlight/src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule #to be updated by Giuseppe (for the IDE)
	cd $HOME/NetIDE/floodlight
	#to solve the 6653 bug
	cp $HOME/NetIDE/Controller.java $HOME/NetIDE/floodlight/src/main/java/net/floodlightcontroller/core/internal/Controller.java #to be updated by Giuseppe (for the IDE)
	make
	cp $HOME/NetIDE/floodlight.sh $HOME/NetIDE/floodlight/floodlight.sh
	sleep 5

	echo "---Final checks..."
	cd $HOME/NetIDE/Engine-mixed/floodlight-backend/v1.2/test
	cp FLCompositionSpecification.xml $HOME/NetIDE/Engine-mixed/ryu-backend/tests/	#copy to Python core
	cp FLCompositionSpecification.xml $HOME/NetIDE/Engine-mixed/apache-karaf-3.0.5/	#copy to Java core

fi

cd $HOME/NetIDE/Engine-mixed
sudo chmod +777 -R *
exit 0




