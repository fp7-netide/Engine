#!/bin/bash
#Creates the basic structure of NetIDE Engine
#[It is used to set up the Vagrant VM]

NetIDE_DIR="$HOME/NetIDE"

if [ "$1" == "-h" ] || [ "$1" == "--help" ]
then
	echo "Usage: setEngine.sh"
	exit 0
fi

if [ -d $NetIDE_DIR ]; then
        echo "NetIDE folder already exists"
	exit 0
fi

echo "--"
echo "-- Installing required packets:"
echo "--"
sudo apt-get -y install git python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev python-zmq ant
sudo apt-get -y remove openvswitch-controller
sudo pip install ecdsa stevedore greenlet oslo.config eventlet WebOb Routes msgpack-python

echo "--"
echo "-- Cloning NetIDE Engine:"
echo "--"
mkdir $NetIDE_DIR
cd $NetIDE_DIR
git clone https://github.com/fp7-netide/Engine.git

echo "--"
echo "-- Installing Mininet (2.2.1):"
echo "--"
mkdir mininet
cd mininet
git clone git://github.com/mininet/mininet.git
if [ -d "mininet" ]; then
        cd mininet
        git checkout 2.2.1rc1
        cd util
        ./install.sh -a
        cd ../../..
fi

echo "--"
echo "-- Installing Java 8 (Oracle and OpenJDK):"
echo "--"
sudo add-apt-repository -y ppa:webupd8team/java
sudo add-apt-repository -y ppa:openjdk-r/ppa
sudo apt-get update
sudo echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections
sudo apt-get install -y oracle-java8-installer
sudo apt-get install -y oracle-java8-set-default
sudo apt-get install -y openjdk-8-jdk

echo "--"
echo "-- Installing Apache Maven (3.3.9):"
echo "--"
wget -q http://apache.websitebeheerjd.nl/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.zip
unzip -q apache-maven-3.3.9-bin.zip
rm -f apache-maven-3.3.9-bin.zip
echo 'export M2_HOME=$HOME/NetIDE/apache-maven-3.3.9' >> ~/.bashrc
echo 'export PATH=$PATH:$M2_HOME/bin' >> ~/.bashrc
export PATH=$PATH:$HOME/NetIDE/apache-maven-3.3.9/bin

echo "--"
echo "-- Installing Apache Karaf (3.0.5):"
echo "--"
wget http://archive.apache.org/dist/karaf/3.0.5/apache-karaf-3.0.5.tar.gz
tar -zxvf apache-karaf-3.0.5.tar.gz
rm -f apache-karaf-3.0.5.tar.gz

echo "--"
echo "-- Installing Ryu (3.27):"
echo "--"
git clone git://github.com/osrg/ryu.git
if [ -d "ryu" ]; then
	cd ryu
	git checkout v3.27
	sudo python ./setup.py install
	cd ..
fi

echo "--"
echo "-- Installing ONOS (1.4):"
echo "--"
sudo update-alternatives --set java /usr/lib/jvm/java-8-oracle/jre/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-8-oracle/bin/javac
git clone https://gerrit.onosproject.org/onos
if [ -d "onos" ]; then
	cd onos
	git checkout onos-1.4
	mvn clean install -DskipTests
	cd ..
	echo 'export ONOS_ROOT=$HOME/NetIDE/onos' >> ~/.bashrc
	echo 'source $ONOS_ROOT/tools/dev/bash_profile' >> ~/.bashrc
	echo 'export ONOS_USER=karaf' >> ~/.bashrc
	echo 'export ONOS_GROUP=karaf' >> ~/.bashrc
	echo 'export ONOS_WEB_USER=karaf' >> ~/.bashrc
	echo 'export ONOS_WEB_PASS=karaf' >> ~/.bashrc
fi

echo "--"
echo "-- Installing Floodlight (1.1):"
echo "--"
git clone https://github.com/floodlight/floodlight.git
if [ -d "floodlight" ]; then
	cd floodlight
	git checkout v1.1
	make
	cd ..
fi

echo "--"
echo "-- Moving updateEngine.sh and runEngine.sh to NetIDE folder"
echo "--"
cp Engine/utils/scripts/updateEngine.sh .
cp Engine/utils/scripts/runEngine.sh .
sudo chmod +x *.sh
