#Ryu shim layer

The Ryu shim layer is one of the components of the NetIDE Network Engine and is implemented as application for the [Ryu controller](http://osrg.github.io/ryu/) by using the OpenFlow libraries included in the Ryu's [source code](https://github.com/osrg/ryu).  
Differently from previous versions of the Network Engine implementation which leveraged on the protocol used between the [Pyretic backend and the OpenFlow client](http://www.cs.princeton.edu/~jrex/papers/pyretic13.pdf), the current modules (such as shim layer and backend) use the NetIDE Intermediate protocol v1.0 to communicate with each other (see the Appendix below).

## Installation

To use the shim, first clone the Ryu code (from [here](https://github.com/osrg/ryu)) on a local machine and copy the ```netide``` folder into the ```ryu/ryu``` folder. After that, install Ryu by running the command ```python ./setup.py install``` from the ```ryu``` folder.
Then, add the Ryu's installation path to the PYTHONPATH variable in your ~/.profile or ~/.bashrc (e.g. in a Ubuntu 14.04 Linux OS: ```export PYTHONPATH=/usr/local/lib/python2.7/dist-packages/ryu```).

Additional python packages may be required in order to succefully complete the installation procedure. On a Ubuntu 14.04 Linux OS the following must be installed:
* ```apt-get install python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev```
* ```pip install ecdsa```
* ```pip install stevedore```
* ```pip install greenlet```

## Running

In this second implementation of the Network Engine, the shim layer is a server (listening by default on port 41414) and handles the multiple connections from the client controllers through their backend layers. Therefore, to test the Ryu shim layer, simply run it with the following command:
```
ryu-manager ryu-shim.py
```

This command will start the shim layer along with the rest of the Ryu platform. As a result, you will obtain a process that waits for connections from the client controllers on the northbound and from the forwarding devices from the southbound.

**Note:** The Ryu controller platform listens for connections from the switches on the port 6633. Therefore, when using Mininet for testing purposes, start mininet by specifying the controller information as follows:
```
sudo mn --topo linear,4 --controller=remote,ip=IP_ADDRESS,port=6633
```

Where ```ryu-backend.py``` is the module that provides the communication with the ```ryu-shim``` through the Intermediate protocol and IP_ADDRESS is the IP address of the machine where the Ryu and the shim layer are running. The IP address specification is not needed when Ryu and Mininet are running on the same machine.

## Testing

To test the Ryu shim layer it is necessary to run one of the backends provided in this github repository that supports the NetIDE Intermediate protocol v1.0.
For instance, it can be tested with the Ryu backend by using the following command:

``` ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py```

Where ```simple_switch``` is a simple application provided for testing purposes. Other applications can be used as well. Many sample applications are available in the Ryu source tree in the ```ryu/app``` folder.

Within the Mininet CLI, a successful ```pingall``` demonstrates that the hosts are able to comminicate with each others.

## License

See the LICENSE file.

## ChangeLog

ryu-shim: 2015-07-01 Wed Rinor Bytyçi <rinorb@gmail.com>

  * New version with support for the new NetIDE Intermediate protocol.

ryu-shim: 2015-01-13 Tue Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * Updated README by adding a new ```Testing``` session

ryu-shim: 2014-12-04 Thu Antonio Marsico <antonio.marsico@create-net.org>

  * Renamed the project from ryu-client into ryu-shim
  * Added support for LLDP messages

ryu-client: 2014-08-27 Wed Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * First public release of the ryu-client. Tested with the master branch of Pyretic (commit 376f63a6d249c9a2e434b87f565982cab24fb6ad of 7th Aug 2014)

# Appendix - Short overview to the NetIDE Intermediate protocol v1.0

The intermediate protocol serves several needs. It has to
(i) carry control messages between the modules of the Network Engine (such as shim and backend), e.g., to start up/take down a particular module, providing
unique identifiers for modules, (ii) carry event and action
messages between shim and backend, properly demultiplexing such messages to the right module based on identifiers, (iii) encapsulate messages specific to a particular SBI
protocol version (e.g., OF 1.X, NETCONF, etc.) towards the
client controllers with proper information to recognize these
messages as such.
In the first prototypes of the Network Engine, we lever-
aged the protocol between [Pyretic’s](http://www.cs.princeton.edu/~jrex/papers/pyretic13.pdf) runtime system and
the underlying OpenFlow client. Although this “Pyretic protocol” was sufficient to accomplish our preliminary proofs of
concept, its current version limits the network applications
running on top of the Network Engine to only use a subset
of OF v1.0 messages and its definition does not provide the
necessary functions required by the composition mechanism
running in the core layer. Especially considering the latter limitation, we defined a new intermediate protocol fromscratch that ensures the delivery of control messages and that
supports different SBI protocols. The protocol uses TCP as
a transport and encapsulates the payload with the following
header:
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|   netide_ver  |     type      |            length             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                              xid                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           module_id                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
+                          datapath_id                          |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
Where ```netide_ver``` is the version of the NetIDE protocol,
```length``` is the total length of the payload in bytes and ```type```
indicates the type of the message (e.g ```NETIDE_HELLO```,
```NETIDE_OPENFLOW```, etc.). ```datapath_id``` is a 64-bits
field that uniquely identifies the network elements.
```module_id``` is a 32-bits field that uniquely identifies the
application modules running on top of each client controller.
The composition mechanism in the core leverages on this
field to implement the correct execution flow of these modules. Finally, ```xid``` is the transaction identifier associated to
the each message. Replies must use the same value to facilitate the pairing.
