# FLOODLIGHT BACKEND
The Floodlight backend is one of the components of the NetIDE Network Engine and is implemented as application for the [Floodlight controller](http://www.projectfloodlight.org/floodlight/).

Differently from previous versions of the Network Engine implementation which leveraged on the protocol used between the [Pyretic backend and the OpenFlow client](http://www.cs.princeton.edu/~jrex/papers/pyretic13.pdf), the current modules (such as shim layer and backend) use the NetIDE Intermediate protocol v1.X to communicate with each other (see a short description in the [Network Engine introduction](https://github.com/fp7-netide/Engine)).


## Installation
* Clone locally Floodlight (https://github.com/floodlight/floodlight.git) and then switch to v1.1 version:

```
git checkout v1.1
```

* Build floodlight using make
```
make
```

* Copy the package net.floodlight.interceptor in the floodlight source folder. (Copy floodlight-backend/v1.2/src/main/java/net/floodlightcontroller/interceptor folder to floodlight/src/main/java/net/floodlightcontroller)

* Copy the jar files under lib folder in floodlight lib folder (Except floodlight.jar used only during development for compilation)

* Replace /floodlightv1.1/src/main/resources/floodlightdefault.properties with the given one that can be found at:
	/floodlight-backend/v1.2/src/main/resources/floodlightdefault.properties

* Add to build.xml of floodlightV1.1 the following lines inside `<patternset id="lib">` tag:

```xml
<include name="jeromq-0.3.4.jar"/>
<include name="javatuples-1.2.jar"/>
<include name="netip-1.1.0-SNAPSHOT.jar"/>
```
(or directly replace the file with the one provided in ```manual/build.xml```)

* Add to /floodlightv1.1/src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule the following line:
	net.floodlightcontroller.interceptor.NetIdeModule
(or manually replace the file with the one provided in ```manual/IFloodlightModule```)

* Compile the project with make command


## Testing

To test it, we will need the Python Core and a Shim (ODL or RYU shim)

### Python Core
Using the FLCompositionSpecification.xml under floodlight-backend/v1.2/test

```python AdvancedProxyCore -c FLCompositionSpecification.xml```


### ODL Shim

* Clone the repo from https://github.com/opendaylight/netide

* Build it using
	```mvn clean install```

* Go to karaf/target/assembly/bin

* Start karaf with (Prior you have to setup JAVA_HOME env variable to the correct path).

* Install netide feature with:
	```feature:install odl-netide-rest```



### Mininet Topology:
* Create mininet topology using the following command:
	```sudo mn --custom netide-topo.py --topo mytopo --controller=remote,ip=127.0.0.1,port=6644```

* Wait until the virtual switches are connected to the shim.



### Start Floodlight
Use the script provided in the floodlight folder floodlight.sh to start the controller.
If the script returns an error related to java options used to start floodlight, replace the 14 line of the script with the following:

```
JVM_OPTS="$JVM_OPTS -XX:CompileThreshold=1500" #-XX:PreBlockSpin=8"
```
(or manually replace the file with the one provided in ```manual/floodlight.sh```)

Test that everything is working fine using pingall from mininet shell.

### KNOWN BUG:
If, starting floodlight, you receive an Error of Manager address already in use please manually change the manager port used by floodlight in Class
net.floodlightcontroller.core.internal.Controller line 136 from  6653 to 7753.
(or manually replace the file with the one provided in ```manual/Controller.java``` and recompile the project)

