# Backend for the Ryu platform

The Ryu backend is one of the components of the NetIDE Network Engine and is implemented as application for the [Ryu controller](http://osrg.github.io/ryu/) by using the OpenFlow libraries included in the Ryu's [source code](https://github.com/osrg/ryu).  
Differently from previous versions of the Network Engine implementation which leveraged on the protocol used between the [Pyretic backend and the OpenFlow client](http://www.cs.princeton.edu/~jrex/papers/pyretic13.pdf), the current modules (such as shim layer and backend) use the NetIDE Intermediate protocol v1.X to communicate with each other (see a short description in the [Network Engine introduction](https://github.com/fp7-netide/Engine)).

## Installation

The Ryu backend is provided as an additional module for the Ryu controller. In order to use it, first clone the Ryu code (from [here](https://github.com/osrg/ryu)) on a local machine. Copy the ```python``` folder from ```../Libraries/netip``` into the ```ryu/ryu``` folder and rename it as ```netide```. After that, install Ryu by running the command ```python ./setup.py install``` from the ```ryu``` folder.
Then, add the Ryu's installation path to the PYTHONPATH variable in your ~/.profile or ~/.bashrc (e.g. in a Ubuntu 14.04 Linux OS: ```export PYTHONPATH=/usr/local/lib/python2.7/dist-packages/ryu```).

Finally, install the Ryu controller by entering the ```ryu``` folder and by running the command:

```python ./setup.py install```

Additional python packages may be required in order to succefully complete the installation procedure. On a Ubuntu 14.04 Linux OS the following must be installed:
* ```sudo apt-get install python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev python-zmq```
* ```sudo pip install ecdsa stevedore greenlet oslo.config eventlet WebOb Routes```

## Running
Within the  ```ryu-backend``` folder, run the following command to use the Ryu backend with the, e.g., ```simple_switch ``` application on top of it:

``` ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py tests/firewall.py```

The Ryu backend will try to connect to a running NetIDE Core which must be already running and listening on the TCP port 5555.
The ```--ofp-tcp-listen-port 7733``` is used to avoid possible conflicts that may happen when two different controller platforms are running on the same machine. In our case we could have Ryu hosting the backend and, e.g., ONOS with the shim layer. By default they both bind the TCP port 6633 to accept connections from the network elements.

Finally, ```simple_switch``` is a simple L2 learning switch application and ```firewall.py``` is a simple firewall application, both provided for testing purposes. Other applications can be used as well. Many sample applications are available in the Ryu source tree in the ```ryu/app``` folder.

## Testing

To test the Ryu backend it is necessary to run one of the shim layers provided in this github repository and the NetIDE Core. Both must support the NetIDE Intermediate protocol v1.2 or later.
The Java implementation of the Core can be found in this repository within folder ```core```.
For instance, to use this backend with the Ryu shim, first start the Core by following the accompanying README and then, run the ```ryu-shim``` with the following command:
```
ryu-manager ryu-shim.py
```

A network emulator such as Mininet can be used to test the software. In the ```test``` folder a script (```netide-topo.py```) that automatically configures Mininet with a 4 switches and 4 hosts topology.
```
sudo mn --custom netide-topo.py --topo mytopo --controller=remote,ip=IP_ADDRESS,port=6633
```
Where IP_ADDRESS is the IP address of the machine where the Ryu shim layer is running. The IP address specification is not needed when Ryu and Mininet are running on the same machine. Add options ```--switch ovs,protocols=OpenFlow13``` if you want to use the OpenFlow-1.3 protocol and test applications.

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

Once both Core and Ryu shim are running, the backend and OpenFlow 1.0 network applications can be started with:

``` ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py tests/firewall.py```

or

``` ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch_13.py tests/firewall_13.py```

if you want to test the OpenFlow 1.3 applications.

For instance, the composition configuration for the Core could assign switches ```S21```, ```S22``` and ```S23``` to the ```simple_switch``` application, while the ```S11``` to the ```firewall``` application.

As an alternative, one may want to test different applications running on different instances of the client controller. In this case, just open two terminals and run the following commands (one for each terminal):

```
ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py
ryu-manager --ofp-tcp-listen-port 7734 ryu-backend.py tests/firewall.py
```

Within the Mininet CLI, a ```pingall``` command demonstrates what traffic is allowed and what is not, depending on the rules installed by the firewall application.

## License

See the LICENSE file.

## ChangeLog

ryu-backend: 2015-11-11 Wed Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * Added OpenFlow 1.3 test applications

ryu-backend: 2015-11-09 Mon Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * Improved heartbeat management
  * Several fixes in both ryu-backend and AdvancedProxyCore

ryu-backend: 2015-10-30 Fri Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * Improved AdvancedProxyCore with support to heartbeat
  * Added the ```NETIDE_HEARTBEAT``` message type to the ```netip``` library
  * Improved ```ryu-backend``` and bug fixing.

ryu-backend: 2015-10-01 Thu Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * Replaced sockets with zeromq networking library
  * Added support to the NetIDE Intermediate protocol specification v1.2

ryu-backend: 2015-09-01 Tue Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * Added support to the NetIDE Intermediate protocol specification v1.1
  * Moved the NetIDE protocol-specific methods into a dedicated library

ryu-backend: 2015-07-01 Wed Rinor Bytyçi <rinorb@gmail.com>

  * New version with support for the new NetIDE Intermediate protocol.

ryu-backend: 2015-01-13 Tue Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

* Updated README

ryu-backend: 2014-11-13 Thu Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

* First working release of the Ryu backend. Tested with the ```simple_switch``` application, mininet and the POX client developed by the Pyretic team (http://frenetic-lang.org/pyretic/)

ryu-backend: 2014-10-21 Tue Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

* First release of the development branch. Not ready for testing.
