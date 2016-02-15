# NetIDE Core
Base package: eu.netide.*

## Project Structure
The core project is built on top of Apache Karaf and is therefore implemented as OSGi bundles. The core currently consists of the following modules:
- core.api
- core.caos
- core.connectivity
- core.management

The modules are built using Maven, the root pom.xml combines them into the core project.

### Module core.api
This bundle contains all shared interfaces and classes for the core, structured into the following Java packages:
- core.api: Shared interfaces and classes for core services
- core.api.netip: Classes for parsing and creating messages using the NetIDE Intermediate Protocol

This bundle does not have any OSGi-dependencies (except for the framework itself), but embeds OpenFlowJ.

### Module core.caos
This bundle contains the composition and conflict resolution logic and related services inside the core.caos package in the following packages:
- core.caos: Runtime logic and services for composition and conflict resolution
- core.caos.composition: Classes for the (de-)serialization of composition specifications and their in-memory representation

It has a dependency on the core.api bundle and embeds JDOM2.

### Module core.connectivity
This bundle implements the services necessary to establish a connection with the shim and backends. Currently, it implements a ZeroMQ-based connector inside the core.connectivity package.
It has a dependency on the core.api bundle and embeds JeroMQ.

### Module core.management
This bundle implements the management interface for the core using ZeroMQ transport and a yet-to-be-determined protocol. It allows to set and query configuration data. Internally, it uses Blueprint configuration to dynamically adapt the running core.
It has a dependency on the core.api and org.apache.karaf.config.core bundles and embeds JeroMQ and org.json.

### Module core.logpub
This bundle contains the LogPub module of the core. It has a interprocess queue to receive the messages pushed by the core.connectivity and publish them to a PUB queue.
The external tools (like the Logger) will subscribe to that queue.

## Used frameworks and technologies
- JDK 8
- Java NetIP library (see [lib](../lib) folder)
- Maven for building the projects (especially the maven-bundle-plugin)
- Apache Karaf as the runtime OSGi container (currently v3.0.3)
- Apache Aries Blueprint for service discovery and injection
- Apache Aries Blueprint Configuration (using Karaf's ConfigAdmin service) for dynamic adaption to configuration changes
	- In cooperation with Karaf's JMX MBeanServer for configuration changes through code
- ZeroMQ for external interfaces (using the Java-only JeroMQ library)
- OpenFlowJ from the ONOS project to parse and generate OpenFlow messages
- org.json for parsing and creating payloads for ManagementMessages
- SLF4J for logging (provided by Karaf)

## How to deploy
### Prerequisites
- The core requires the Java NetIP library in your local Maven repository for a successful build. Therefore, go to the [libraries/netip/java](../lib/netip/java) directory and run `mvn clean install` before continuing.

### Core Deployment 
1. Clone the *CoreImplementation* branch of the repository to your machine.
2. Go to the *core* directory and run `mvn clean install`. This will build the bundles and install them to your local Maven repository.
3. Download [Apache Karaf](https://karaf.apache.org/index/community/download.html). (we use v3.0.3 since both ODL and ONOS use that same version)
4. Start Karaf by going into the downloaded folder and running `bin/karaf`.
5. Install the *netide-core* feature by first adding the feature repository file via `feature:repo-add mvn:eu.netide.core/core/1.0.1.0-SNAPSHOT/xml/features` and then running `feature:install netide-core`.
	- The output shold indicate that the core is waiting for the shim to connect.

If you need to refresh the modules in karaf use `bundle:watch *` (This works only if the module versions have a -SNAPSHOT suffix)
	
### Core Deployment + Composition Specification
If we want to include some composition specification, then we have to follow some intermediate steps:

1. Clone the *CoreImplementation* branch of the repository to your machine.
2. Go to the *core* directory and run `mvn clean install`. This will build the bundles and install them to your local Maven repository.
3. Go to the *tools/emulator* inside the *core* directory and execute `mvn package`. Then, back to *core* and run `mvn clean install` again. This will create the emulator package, which lets the user introduce a composition specification file defined in *.xml.
4. Download [Apache Karaf](https://karaf.apache.org/index/community/download.html). (we use v3.0.3 since both ODL and ONOS use that same version)
5. Start Karaf by going into the downloaded folder and running `bin/karaf`.
6. Install the *netide-core* feature by first adding the feature repository file via `feature:repo-add mvn:eu.netide.core/core/1.0.1.0-SNAPSHOT/xml/features` and then running `feature:install netide-core`.
	- The output shold indicate that the core is waiting for the shim to connect.
7. Copy the composition file into the *tools/emulator/target* folder in the *core* (the *xml should be in the same folder than the generated *.jar). There is an example called `MinimalSpecification.xml` in the *tools* folder, which defines a single switch application module. 
8. Go to *tools/emulator/target* and execute the emulator package to send the composition file to the running core `java -jar emulator-1.0-jar-with-dependencies.jar`. The emulator will request you to enter some parameters, an example is shown below:
```
Demo started.
How do you want to identify? (shim, backendX; default=shim)
> backend1
To which port do you want to connect? (default=5555)
> 5555
Which socket type do you want to use? (dealer,sub;default=dealer)
> dealer
Enter command: (packetIn (p), composition (c), announcement (a), acknowledge (ack), flowmod (f), requestend (r), exit)
> Connected to localhost:5555 as 'backend1' using a DEALER socket.
c
Which file?
> MinimalSpecification.xml
To which port? (default=5556)
> 5556
Sending composition specification '/home/tamu/netide/CoreImplementation/core/tools/emulator/target/MinimalSpecification.xml' to management interface at localhost:5556...
Sending:
{
   "parameters": {
      "pid": "eu.netide.core.caos",
      "value": "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<CompositionSpecification  xmlns=\"http://netide.eu/schemas/compositionspecification/v1\">\n  <Modules>\n    <Module id=\"SimpleSwitch\" loaderIdentification=\"simple_switch.py\"/>\n  <\/Modules>\n  <Composition>\n    <ModuleCall module=\"SimpleSwitch\"/>\n  <\/Composition>\n<\/CompositionSpecification>\n",
      "key": "compositionSpecification"
   },
   "command": "set-configvalue"
}
Sent.
```
9. Run now Mininet, the shim and finally the backend (Note: If you are using the example composition file, you should use the Ryu `simple_switch.py` application)
 

### Composition specification via karaf
```
karaf@root()>  netide:loadcomposition /path/to/composition
```
 
### Show connected backends/modules
```
  karaf@root()> netide:listmodules 
     Id                 Name              Backend Last message (s ago)
    797         SimpleSwitch    backend-ryu-14172                    -
    238    backend-ryu-14172    backend-ryu-14172                 4,66
```
