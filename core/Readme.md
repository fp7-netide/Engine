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
- The core requires the Java NetIP library in your local Maven repository for a successful build. Therefore, go to the [libraries/netip/java](../lib/netip/java) directory and run `mvn clean install` before continuing. [Important note: The NetIP library should have the exact same location as it is now in the GitHub Engine repository: relative path to core/ is `../libraries/netip/java', otherwise compiling the core project afterwards will fail, because the pom.xml directly references it in that location]

### Core Deployment 
1. Clone the *master* branch of the repository to your machine.
2. Go to the *core* directory and run `mvn clean install`. This will build the bundles and install them to your local Maven repository.
3. Download [Apache Karaf](https://karaf.apache.org/index/community/download.html) v3.0.5
	- Optional: Install the branding osgi packet to the lib directory of karaf
	```cp core.branding/target/core.branding-1.1.0-SNAPSHOT.jar ~/dl/apache-karaf-3.0.5/lib/``
4. Start Karaf by going into the downloaded folder and running `bin/karaf`.
<p align="center">
  <img src="https://raw.githubusercontent.com/fp7-netide/Engine/master/core/doc/branding.png" alt="Branding Apache Karaf"/>
</p>
5. Install the *netide-core* feature by first adding the feature repository file via `feature:repo-add mvn:eu.netide.core/core.features/1.1.0-SNAPSHOT/xml/features` and then running `feature:install core`.
	- The output shold indicate that the core is waiting for the shim to connect.
6. Load a composition specification with netide:loadcomposition, for example the Minimal Specification
```
karaf@root()>  netide:loadcomposition /home/tamu/netide/CoreImplementation/core/tools/emulator/target/MinimalSpecification.xml
```
7. Run now Mininet, the shim and finally the backend (Note: If you are using the example composition file, you should use the Ryu `simple_switch.py` application)
 
### Developing
To make karaf reload changed bundles after a mvn install , tell karaf to watch the bundles by `bundle:watch *`. This works only if the core version has a -SNAPSHOT suffix)

 
### Show connected backends/modules
```
  karaf@root()> netide:listmodules 
     Id                 Name              Backend Last message (s ago)
    797         SimpleSwitch    backend-ryu-14172                    -
    238    backend-ryu-14172    backend-ryu-14172                 4,66
      -                 shim                    -                 0,32
```
