#Backend for the Ryu SDN framework

The API interceptor is a component of the NetIDE architecture that allows SDN applications written for different SDN controllers to be executed on top of OpenDayLight (ODL).

The API interceptor is composed of two different modules: (i) the ODL shim client which is an application for ODL written in Java and (ii) the SDN controller-specific backend implemented with the language of the controller (Python for Ryu, Java for FloodLight etc.).

The two modules (backend and shim client) talk each other through a TCP socket by using the same APIs used by Pyretic (just for the first tests) to talk with the of_clients.

To summarize with an example: when a packet_in message arrives to ODL from a switch, the message is converted by the ODL shim client into a Pyretic-like message and then sent to the above backend module of the, e.g. Ryu, controller. Finally the message is passed to the application that decides how to forward the related flow through either a packet_out or a flow_mod answer.

#Installation

The Ryu backend is provided as an additional module for the Ryu controller. In order to use it, download the Ryu's source code first (```git clone https://github.com/osrg/ryu.git```).
After that, copy the ```backend``` folder into the ```ryu/ryu``` folder just downloaded and add the Ryu's code path to the PYTHONPATH variable (e.g. in ~/.profile file). Finally, install the Ryu controller by entering the ```ryu``` folder and by running the command:

```python ./setup.py install```

Additional python packages may be required in order to succefully complete the installation procedure. On a Ubuntu 14.04 Linux OS the following must be installed: 
* ```apt-get install python-pip python-dev python-repoze.lru```
* ```pip install ecdsa```
* ```pip install stevedore```
* ```pip install greenlet```

#Running
From the  ryu's code folder, run the following command to use the Ryu backend with the, e.g., ```simple_switch ``` application on top of it (port 7733 is used to avoid conflicts with the ODL shim client):

``` ryu-manager --ofp-tcp-listen-port 7733 ryu/backend/backend.py ryu/app/simple_switch.py```

then start the ODL shim client (or use the POX client as explained in the "Testing" section below).

#Testing

The folder ```tests``` contains the tools to test the Ryu backend (and any other backend with the same southbound interface).

## The POX client

The POX client can replace the ODL shim client in the API interceptor implementation during the development and testing phases. It comes from the Pyretic source code (https://github.com/frenetic-lang/pyretic) and has been modified in order to remove the Pyretic dependencies. Moreover, it is continuously updated to be compliant with the improvements of the backend southbound APIs.
To use the POX client follow this procedure:

* download the source code of POX (https://github.com/noxrepo/pox.git)
* copy the file pox_client.py into the ```pox/ext``` folder
* run the ryu backend as described in the "Running" section of this README
* enter the pox folder and run ```./pox.py pox_client"```
 


#ChangeLog

ryu-backend: 2015-01-13 Tue Roberto Doriguzzi Corin roberto.doriguzzi@create-net.org

* Updated README

ryu-backend: 2014-11-13 Thu Roberto Doriguzzi Corin roberto.doriguzzi@create-net.org

* First working release of the Ryu backend. Tested with the ```simple_switch``` application, mininet and the POX client developed by the Pyretic team (http://frenetic-lang.org/pyretic/)

ryu-backend: 2014-10-21 Tue Roberto Doriguzzi Corin roberto.doriguzzi@create-net.org

* First release of the development branch. Not ready for testing.
