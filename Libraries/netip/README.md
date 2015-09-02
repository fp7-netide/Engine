## The NetIDE Intermediate protocol v1.1

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
running in the core layer. Especially considering the latter limitation, we defined a new intermediate protocol from scratch that ensures the delivery of control messages and that
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
