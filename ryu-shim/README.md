#Ryu shim layer

The Ryu shim layer is one of the two layers of the NetIDE API interceptor and is implemented as application for the [Ryu controller](http://osrg.github.io/ryu/) by using the OpenFlow libraries included in the Ryu's [source code](https://github.com/osrg/ryu).  

## Installation

To use the client, first clone the Ryu code (from [here](https://github.com/osrg/ryu)) on a local machine and install Ryu by following the procedure described in this [README](https://github.com/osrg/ryu/blob/master/README.rst) file.
After that, do not forget to add the Ryu's code path to the ```PYTHONPATH``` variable (e.g. in ```~/.profile``` file).

## Running

In the current implementation of the NetIDE API interceptor, the shim layer is the client of the TCP connection while the backend is the server (listening by default on port 41414). Therefore, to test the Ryu shim layer, run the backend first (see the Ryu and FloodLight backends READMEs) and than start the shim layer with the following command:
```
ryu-manager ryu-shim.py
```
The Ryu shim layer listens for connections from the switches on the port 6633. Therefore, when using mininet for testing purposes, start mininet by specifying the controller information as follows:
```
sudo mn --custom netenv.py --topo netenv  --controller remote,port=6633
```


## TODO

* The code is in alpha version and has been tested with simple topologies. 

## License

See the LICENSE file.

## ChangeLog

ryu-shim: 2014-12-04 Thu Antonio Marsico <antonio.marsico@create-net.org>

  * Renamed the project from ryu-client into ryu-shim
  * Added support for LLDP messages

ryu-client: 2014-08-27 Wed Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * First public release of the ryu-client. Tested with the master branch of Pyretic (commit 376f63a6d249c9a2e434b87f565982cab24fb6ad of 7th Aug 2014) 


