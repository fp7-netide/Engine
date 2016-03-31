# ONOS shim layer

The ONOS shim layer is implemented as a bundle for the [ONOS controller](http://onosproject.org/) by using the libraries included in its [source code](https://wiki.onosproject.org/display/ONOS/Downloads).  

## Installation

### STEP 1: Download ONOS
```
$ git clone https://gerrit.onosproject.org/onos
$ git checkout v1.4.0
```
### STEP 2: ONOS Prerequisites

Follow the ONOS WiKi (https://wiki.onosproject.org/display/ONOS/Installing+and+Running+ONOS) in order to setup your environment to compile and run ONOS

### STEP 3: Create a custom ONOS cell

Please refer to the official guide [click here](https://wiki.onosproject.org/display/ONOS/ONOS+from+Scratch#ONOSfromScratch-4.Createacustomcelldefinition)

In this way you can easily deploy ONOS on a VM machine or in a local environment. Rember to issue the command ```cell *your cell name*``` before continue with this guide. This loads all the environment variables for ONOS in your shell.

### STEP 4: Download and Install ONOS shim application

The ONOS shim is a standalone project. It can be easily compiled by Maven and installed in the ONOS controller.

Download the code:
```
$ git clone https://github.com/fp7-netide/Engine.git
```

Enter the project directory and compile the code:
```
$ cd onos-shim && mvn clean install -DskipTests
```


## Run a test

### 1. Prerequisites

- Mininet VM version >= 2.2.0 is required. Please refer to this [guide](http://mininet.org/download/#option-1-mininet-vm-installation-easy-recommended).

- The ONOS shim requires the Java NetIP library in your local Maven repository for a successful build. Therefore, go to the [libraries/netip/java](../lib/netip/java) directory and run `mvn clean install` before continuing.

### 2. Set up your test environment

#### 2.1 Ryu installation

The Ryu backend is provided as an additional module for the Ryu controller. In order to use it, first clone the Ryu code (from [here](https://github.com/osrg/ryu)) on a local machine. Copy the ```python``` folder from ```../Libraries/netip``` into the ```ryu/ryu``` folder and rename it as ```netide```. After that, install Ryu by running the command ```python ./setup.py install``` from the ```ryu``` folder.
Then, add the Ryu's installation path to the PYTHONPATH variable in your ~/.profile or ~/.bashrc (e.g. in a Ubuntu 14.04 Linux OS: ```export PYTHONPATH=/usr/local/lib/python2.7/dist-packages/ryu```).

Finally, install the Ryu controller by entering the ```ryu``` folder and by running the command:

```python ./setup.py install```

Additional python packages may be required in order to succefully complete the installation procedure. On a Ubuntu 14.04 Linux OS the following must be installed:
* ```sudo apt-get install python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev python-zmq```
* ```sudo pip install ecdsa stevedore greenlet oslo.config eventlet WebOb Routes lxml```

#### 2.2 ONOS setup

ONOS can be run on your local machine or on a VM. If you successfully complete the procedure in [ONOS Prerequisites](#step-2-onos-prerequisites), now you can start ONOS in your local system with this command:

```
$ ok clean
```

or you can access the VM where ONOS is deployed.

Once ONOS is running, the configuration of FlowRuleManager must be modified in order to permit network rules that are not installed directly by ONOS. The following commands must be issued in the Karaf shell of ONOS:

```
onos> onos:cfg set org.onosproject.net.flow.impl.FlowRuleManager allowExtraneousRules true
```

#### 2.3 ONOS shim installation

In order to install the ONOS shim, the following command is required in the `onos-shim` folder:

```$ onos-app $OC1 install target/onos-app-shim-1.0.0-SNAPSHOT.oar```

Where `$OC1` represents the ONOS IP address that should be set in the `cell` file, as described in [Create a custom ONOS cell](#step-3-create-a-custom-onos-cell)

In order to modify the CORE address (default is *localhost:5555*), the following commands can be used in the Karaf shell of ONOS:

```
onos> config:edit eu.netide.shim.ShimLayer

onos> property-set coreAddress *CORE IP*

onos> property-set corePort *CORE PORT*

onos> config:update
```

### 3. Running
The ONOS shim application can be tested with the `ryu-backend`. In the ```ryu-backend``` folder, run the following command to use the Ryu backend with the, e.g., ```simple_switch ``` application on top of it:

``` ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py tests/firewall.py```

The Ryu backend will try to connect to a running NetIDE Core which must be already running and listening on the TCP port 5555.
The ```--ofp-tcp-listen-port 7733``` is used to avoid possible conflicts that may happen when two different controller platforms are running on the same machine. In our case we could have Ryu hosting the backend and, e.g., ONOS with the shim layer. By default they both bind the TCP port 6633 to accept connections from the network elements.

Finally, ```simple_switch``` is a simple L2 learning switch application and ```firewall.py``` is a simple firewall application, both provided for testing purposes. Other applications can be used as well. Many sample applications are available in the Ryu source tree in the ```ryu/app``` folder.

### 4. Testing

To test the ONOS shim it is necessary to run one of the backends provided in this Github repository and the NetIDE Core. Both must support the NetIDE Intermediate protocol v1.2 or later.
The Java implementation of the Core can be found in this repository within folder ```core```.
For instance, to use this shim with the Ryu backend, first start the Core by following the accompanying README and then, run the following command:

```
onos> app activate eu.netide.shim
```

A network emulator such as Mininet can be used to test the software. In the ```test``` folder a script (```netide-topo.py```) that automatically configures Mininet with a 4 switches and 4 hosts topology.
```
$ sudo mn --custom netide-topo.py --topo mytopo --controller=remote,ip=IP_ADDRESS,port=6633
```
Where IP_ADDRESS is the IP address of the machine where the ONOS shim layer is running. The IP address specification is not needed when Ryu and Mininet are running on the same machine. Add options ```--switch ovs,protocols=OpenFlow13``` if you want to use the OpenFlow-1.3 protocol and test applications.

This script configures the following topology:

```
alice alice-eth0:s22-eth1
bob bob-eth0:s22-eth2
charlie charlie-eth0:s11-eth1
www www-eth0:s23-eth1
s11 lo:  s11-eth1:charlie-eth0 s11-eth2:s21-eth1
s21 lo:  s21-eth1:s11-eth2 s21-eth2:s22-eth3 s21-eth3:s23-eth2
s22 lo:  s22-eth1:alice-eth0 s22-eth2:bob-eth0 s22-eth3:s21-eth2
s23 lo:  s23-eth1:www-eth0 s23-eth2:s21-eth3
```

Where ```alice```, ```bob``` and ```www``` belong to a hypothetical LAN protected by a firewall (switch ```s11```), while ```charlie``` is outside the LAN.

Once both Core and ONOS shim are running, the backend and OpenFlow 1.0 network applications can be started with:

``` $ ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py tests/firewall.py```

If you want to test the OpenFlow 1.3 applications:

``` $ ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch_13.py tests/firewall_13.py```

For instance, the composition configuration for the Core could assign switches ```S21```, ```S22``` and ```S23``` to the ```simple_switch``` application, while the ```S11``` to the ```firewall``` application.

As an alternative, one may want to test different applications running on different instances of the client controller. In this case, just open two terminals and run the following commands (one for each terminal):

```
$ ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py
$ ryu-manager --ofp-tcp-listen-port 7734 ryu-backend.py tests/firewall.py
```

Within the Mininet CLI, a ```pingall``` command demonstrates what traffic is allowed and what is not, depending on the rules installed by the firewall application.

## License

See the LICENSE file.

## ChangeLog

onos-shim: 2016-02-16 Tue Antonio Marsico <antonio.marsico@create-net.org>

  * Improved ONOS shim with:
    * NetIP library support
    * OF 1.3

onos-shim: 2015-06-15 Mon Matteo Gerola <matteo.gerola@create-net.org>

  * First version of the implementation of the shim for the ONOS platform. It supports ```packet_in```, ```packet_out```, ```flow_mod``` and ```LLDP``` packets.
