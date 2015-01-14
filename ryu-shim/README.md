#Ryu shim layer

The Ryu shim layer is one of the two layers of the NetIDE API interceptor and is implemented as application for the [Ryu controller](http://osrg.github.io/ryu/) by using the OpenFlow libraries included in the Ryu's [source code](https://github.com/osrg/ryu).  

## Installation

To use the shim, first clone the Ryu code (from [here](https://github.com/osrg/ryu)) on a local machine and install Ryu by following the procedure described in this [README](https://github.com/osrg/ryu/blob/master/README.rst) file.
After that, copy the ```backend``` folder into the ```ryu/ryu``` folder just downloaded and add the Ryu's code path to the PYTHONPATH variable in your ~/.profile or ~/.bashrc (e.g. ```export PYTHONPATH="$HOME/ryu"```).

Finally, install the Ryu controller by entering the ```ryu``` folder and by running the command:

```python ./setup.py install```

Additional python packages may be required in order to succefully complete the installation procedure. On a Ubuntu 14.04 Linux OS the following must be installed: 
* ```apt-get install python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev```
* ```pip install ecdsa```
* ```pip install stevedore```
* ```pip install greenlet```

## Running

In the current implementation of the NetIDE API interceptor, the shim layer is the client of the TCP connection while the backend is the server (listening by default on port 41414). Therefore, to test the Ryu shim layer, run the backend first (see the Ryu and FloodLight backends READMEs) and then start the shim layer with the following command:
```
ryu-manager ryu_shim.py
```
The Ryu shim layer listens for connections from the switches on the port 6633. Therefore, when using mininet for testing purposes, start mininet by specifying the controller information as follows:
```
sudo mn --custom netenv.py --topo netenv  --controller remote,port=6633
```
## Testing

The ryu_shim can be tested with the Pyretic framework by replacing the POX client that Pyretic uses to communicate with the OpenFlow switches. After the installation of Ryu described above, clone the Pyretic (from [here](https://github.com/frenetic-lang/pyretic)) source code on the same local machine.

Add the Ryu shim to the Pyretic's source code by:

* copying the ```ryu_shim.py``` file from this repo to the ```pyretic/of_client``` folder
* replacing the original ```pyretic.py``` with the one contained in the ```tests``` folder in order to add support for the Ryu shim (so that Pyretic can launch ryu_shim automatically).

For instance, run the following command to use the Ryu shim and the Pyretic's mac_learner application:
```
./pyretic.py -v low -c ryu  pyretic.modules.mac_learner
```

## TODO

* The code is in alpha version and has been tested with simple topologies. 

## License

See the LICENSE file.

## ChangeLog

ryu-client: 2015-01-13 Tue Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * Updated README by adding a new ```Testing``` session 

ryu-shim: 2014-12-04 Thu Antonio Marsico <antonio.marsico@create-net.org>

  * Renamed the project from ryu-client into ryu-shim
  * Added support for LLDP messages

ryu-client: 2014-08-27 Wed Roberto Doriguzzi Corin <roberto.doriguzzi@create-net.org>

  * First public release of the ryu-client. Tested with the master branch of Pyretic (commit 376f63a6d249c9a2e434b87f565982cab24fb6ad of 7th Aug 2014) 


