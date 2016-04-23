# OpenDaylight Shim Layer

The OpenDaylight shim layer is one of the components of the NetIDE Network Engine and is implemented as a component of the OpenDaylight controller by using the OpenFlow libraries included in the OpenDaylight's source code.
Differently from previous versions of the Network Engine implementation which leveraged on the protocol used between the Pyretic backend and the OpenFlow client, the current modules (such as shim layer and backend) use the NetIDE Intermediate protocol v1.X to communicate with each other (see a short description in the Network Engine introduction).

## Installation

The procedure is tested on an Ubuntu 14.04 machine

Install Java Openjdk 1.7

```sudo apt-get install openjdk-7-jdk```

Install Maven 3.1.x or later. Download packages from [http://maven.apache.org/download.cgi]

```tar -zxf apache-maven-3.x.x-bin.tar.gz```

```sudo cp -R apache-maven-3.x.x /usr/local```

```sudo ln -s /usr/local/apache-maven-3.x.x/bin/mvn /usr/bin/mvn```

Verify Maven installation

```mvn --version```

Edit your ~/.m2/settings.xml

```mkdir -p ~/.m2```

```wget -q -O - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > ~/.m2/settings.xml```

Increase the amount of RAM maven can use

```export MAVEN_OPTS='-Xmx1048m -XX:MaxPermSize=512m'```

Build ODL Shim

```cd odl-shim```

```mvn clean install```


## Running
Start karaf and install the the ODL Shim

```cd odl-shim/karaf/target/assembly/bin```

./karaf

```feature:install odl-netide-rest```

Wait until the following command give an input

```log:display | grep "NetideProvider Session Initiated"```

*Note*: To run another Karaf instance (apart from ODL's), we need to modify the following file: ```odl-shim/karaf/target/assembly/etc/org.apache.karaf``` and manually change ports 1099 and 44444 to something different.

# Testing
To test the ODL shim it is necessary to run one of the backends provided in this github repository and the NetIDE Core. Both must support the NetIDE Intermediate protocol v1.2.
In the ```ryu-backend/tests``` folder, a minimal implementation of the Core is provided.
For instance, to use this shim with the Ryu backend run following sequence of commands:

Run mininet and create the topology

```cd ryu-backend/tests```

```sudo mn --custom netide-topo.py --topo mytopo --controller=remote,ip=127.0.0.1,port=6644```

Run the Core by following the README in [https://github.com/fp7-netide/Engine/tree/master/core].


Run Ryu-backend (with OF1.0 apps). Follow the instructions at [https://github.com/fp7-netide/Engine/tree/master/ryu-backend] to install Ryu-Backend.

```cd Engine/ryu-backend```

```ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py tests/firewall.py```

To test the demo from mininet console execute ```pingall```. The Results should be 8% dropped (11/12 received)
