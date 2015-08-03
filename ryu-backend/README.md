#Backend for the Ryu platform
The Ryu backend is one of the components of the NetIDE Network Engine and is implemented as application for the [Ryu controller](http://osrg.github.io/ryu/) by using the OpenFlow libraries included in the Ryu's [source code](https://github.com/osrg/ryu).  
Differently from previous versions of the Network Engine implementation which leveraged on the protocol used between the [Pyretic backend and the OpenFlow client](http://www.cs.princeton.edu/~jrex/papers/pyretic13.pdf), the current modules (such as shim layer and backend) use the NetIDE Intermediate protocol v1.0 to communicate with each other (see the Appendix below).

#Installation

The Ryu backend is provided as an additional module for the Ryu controller. In order to use it, first clone the Ryu code (from [here](https://github.com/osrg/ryu)) on a local machine and copy the ```netide``` folder into the ```ryu/ryu``` folder. After that, install Ryu by running the command ```python ./setup.py install``` from the ```ryu``` folder.
Then, add the Ryu's installation path to the PYTHONPATH variable in your ~/.profile or ~/.bashrc (e.g. in a Ubuntu 14.04 Linux OS: ```export PYTHONPATH=/usr/local/lib/python2.7/dist-packages/ryu```).

Finally, install the Ryu controller by entering the ```ryu``` folder and by running the command:

```python ./setup.py install```

Additional python packages may be required in order to succefully complete the installation procedure. On a Ubuntu 14.04 Linux OS the following must be installed:
* ```apt-get install python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev```
* ```pip install ecdsa```
* ```pip install stevedore```
* ```pip install greenlet```

#Running
Within the  ```ryu-backend``` folder, run the following command to use the Ryu backend with the, e.g., ```simple_switch ``` application on top of it:

``` ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py```

The Ryu backend will try to connect to a running shim layer which must be already running and listening on the TCP port 41414.
The ```--ofp-tcp-listen-port 7733``` is used to avoid possible conflicts that may happen when two different controller platforms are running on the same machine. In our case we could have Ryu hosting the backend and, e.g., ONOS with the shim layer. By default they both bind the TCP port 6633 to accept connections from the network elements.

Finally, ```simple_switch``` is a simple L2 learning switch application provided for testing purposes. Other applications can be used as well. Many sample applications are available in the Ryu source tree in the ```ryu/app``` folder.

#Testing

To test the Ryu backend it is necessary to run one of the shim layers provided in this github repository that supports the NetIDE Intermediate protocol v1.0.
For instance, it can be tested with the Ryu shim by using the following command:
```
ryu-manager ryu-shim.py
```

A network emulator such as Mininet can be used to test the software:
```
sudo mn --topo linear,4 --controller=remote,ip=IP_ADDRESS,port=6633
```
Where IP_ADDRESS is the IP address of the machine where the Ryu and the shim layer are running. The IP address specification is not needed when Ryu and Mininet are running on the same machine.

Once the Ryu shim is running and listening on TCP port 41414, the backend can be started with:

``` ryu-manager --ofp-tcp-listen-port 7733 ryu-backend.py tests/simple_switch.py```

Within the Mininet CLI, a successful ```pingall``` demonstrates that the hosts are able to comminicate with each others.


#ChangeLog

ryu-backend: 2015-07-01 Wed Rinor Bytyçi <rinorb@gmail.com>

  * New version with support for the new NetIDE Intermediate protocol.

ryu-backend: 2015-01-13 Tue Roberto Doriguzzi Corin roberto.doriguzzi@create-net.org

* Updated README

ryu-backend: 2014-11-13 Thu Roberto Doriguzzi Corin roberto.doriguzzi@create-net.org

* First working release of the Ryu backend. Tested with the ```simple_switch``` application, mininet and the POX client developed by the Pyretic team (http://frenetic-lang.org/pyretic/)

ryu-backend: 2014-10-21 Tue Roberto Doriguzzi Corin roberto.doriguzzi@create-net.org

* First release of the development branch. Not ready for testing.

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
