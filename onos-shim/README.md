# ONOS shim layer

The ONOS shim layer is implemented as a bundle for the [ONOS controller](http://onosproject.org/) by using the libraries included in its [source code](https://wiki.onosproject.org/display/ONOS/Downloads).  

## Installation

STEP 1: Download ONOS

From URL http://downloads.onosproject.org/release/onos-1.2.0.tar.gz, download ONOS 1.2.0 release and unpack it.

STEP 2: ONOS Prerequisites

Follow the ONOS WiKi (“https://wiki.onosproject.org/display/ONOS/Installing+and+Running+ONOS”) in order to setup your environment to compile and run ONOS

STEP 3: Download and Install ONOS Shim App

Copy the ```onos-shim``` folder within the ONOS ```apps``` folder.
Add the ```onos-shim``` in the pom.xml file contained in the ```apps``` folder (```modules``` section):
```
<modules>
	…
        <module>shim</module>
</modules>
```

STEP 4: Compile ONOS
Run ```mvn clean install``` in the ONOS parent folder to compile all the ONOS modules. You should see the ```onos-shim``` app in the list of the compiled apps.

STEP 5: Run ONOS
Run ONOS as explained the link cited in STEP 2. Once logged in Karaf, start the ```onos-shim``` using the command:
```
onos:app activate shim
```


## ChangeLog

onos-client: 2015-06-15 Mon Matteo Gerola <matteo.gerola@create-net.org>

  * First version of the implementation of the shim for the ONOS platform. It supports ```packet_in```, ```packet_out```, ```flow_mod``` and ```LLDP``` packets.