# ONOS Backend Layer

The ONOS backend layer is implemented as a bundle for the [ONOS controller](http://onosproject.org/) by using the libraries included in its [source code](https://wiki.onosproject.org/display/ONOS/Downloads).  
 
## Installation

### STEP 1: Download ONOS
```
$ git clone https://gerrit.onosproject.org/onos
$ git checkout v1.7.1
```
### STEP 2: ONOS Prerequisites

Follow the ONOS WiKi (https://wiki.onosproject.org/display/ONOS/Installing+and+Running+ONOS) in order to setup your environment to compile and run ONOS. Please compile ONOS using `buck build onos`.

### STEP 3: Create a custom ONOS cell

Please refer to the official guide [click here](https://wiki.onosproject.org/display/ONOS/ONOS+from+Scratch#ONOSfromScratch-4.Createacustomcelldefinition)

In this way you can easily deploy ONOS on a VM machine or in a local environment. Rember to issue the command ```cell *your cell name*``` before continue with this guide. This loads all the environment variables for ONOS in your shell.

### STEP 4: Download and Install ONOS backend application

The ONOS backend is a standalone project. It can be easily compiled by Maven and installed in the ONOS controller.

Download the code:
```
$ git clone https://github.com/fp7-netide/Engine.git
```

Enter the project directory and compile the code:
```
$ cd onos-backend
```

```
$ mvn clean install
```

## Run a test

### 1. Prerequisites

- Mininet VM version >= 2.2.0 is required. Please refer to this [guide](http://mininet.org/download/#option-1-mininet-vm-installation-easy-recommended).

- The ONOS backend requires the Java NetIP library in your local Maven repository for a successful build. Therefore, go to the [libraries/netip/java](../lib/netip/java) directory and run `mvn clean install` before continuing.

### 2. Set up your test environment

#### 2.1 Deactivate ONOS OpenFlow provider
Open the ONOS shell and issue the following command:
```
onos> app deactivate org.onosproject.openflow
```

#### 2.2 NetIDE Java Core installation
Please refer to the installation guide of NetIDE Java Core. The ONOS karaf shell can be used to execute the Java Core. In order to make it work with ONOS 1.7.0, use the version of the branch *onos-shim-development* in this repository.

#### 2.3 ONOS shim installation
Please refer to the installation guide of NetIDE ONOS shim

#### 2.4 Activate Mininet
Load the topology as described in Ryu backend README

#### 2.5 ONOS backend installation

In order to install the ONOS backend, the following command is required in the `backend` folder:

```
$ onos-app $OC1 install target/onos-app-backend-1.0.0-SNAPSHOT.oar
```

Where `$OC1` represents the ONOS IP address that should be set in the `cell` file, as described in [Create a custom ONOS cell](#step-3-create-a-custom-onos-cell)

In order to modify the CORE ZeroMQ socket address (default is *localhost:5555*), the following commands can be used in the Karaf shell of ONOS:

```
onos> config:edit eu.netide.backend.BackendLayer

onos> property-set coreAddress *CORE IP*

onos> property-set corePort *CORE PORT*

onos> config:update
```
#### 2.6 Activate ONOS forwarding application and other services from ONOS
In the ONOS shell, issue this command:
```
onos> app activate org.onosproject.fwd org.onosproject.lldpprovider org.onosproject.hostprovider
```

#### 2.7 Activate ONOS backend application
Finally, the following command will activate ONOS backend:
```
onos> app activate eu.netide.backend
```
## Known issues
1. The backend in a multi-switch topology may not correctly register all the switches in the network. **Solution:** Shutdown Mininet and restart it.
