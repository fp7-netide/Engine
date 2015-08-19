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
- Java NetIP library (see [lib](../lib) folder)
- Maven for building the projects (especially the maven-bundle-plugin)
- Apache Karaf as the runtime OSGi container
- Apache Aries Blueprint for service discovery and injection
- Apache Aries Blueprint Configuration (using Karaf's ConfigAdmin service) for dynamic adaption to configuration changes
	- In cooperation with Karaf's JMX MBeanServer for configuration changes through code
- ZeroMQ for external interfaces (using the Java-only JeroMQ library)
- OpenFlowJ from the ONOS project to parse and generate OpenFlow messages
- org.json for parsing and creating payloads for ManagementMessages
- SLF4J for logging (provided by Karaf)

## How to deploy
### Prerequisites
- The core requires the Java NetIP library in your local Maven repository for a successful build. Therefore, go to the [lib/netip/java](../lib/netip/java) directory and run `mvn clean install` before continuing.

### Core Deployment
1. Clone the *CoreImplementation* branch of the repository to your machine.
2. Go to the *core* directory and run `mvn clean install`. This will build the bundles and install them to your local Maven repository.
3. Download [Apache Karaf](https://karaf.apache.org/index/community/download.html). (I recommend at least version 4.0.0.0)
4. Start Karaf by going into the downloaded folder and running `bin/karaf`.
5. Install the *netide-core* feature by first adding the feature repository file via `feature:repo-add mvn:eu.netide.core/core/1.0.0.0/xml/features` and then running `feature:install netide-core`.
	- The output shold indicate that the core is waiting for the shim to connect.
