# NetIDE Core

## Current Status
- Implemented basic socket listener for ryu-shim
	- Implemented as OSGi bundles for Apache Karaf
- Created Apache Karaf feature repository file for easy installation

## TODO
- Message parsing
- Service distribution
- Backend communicator 
- Actual CaOs implementation

## How to test the current code
1. Clone the CoreImplementation branch of the repository to your machine.
2. Go to the *core* directory and run `mvn clean install`. This will build the bundle and install them to your local Maven repository.
3. Download [Apache Karaf](https://karaf.apache.org/index/community/download.html). (I recommend at least version 4.0.0.0)
4. Start Karaf by going into the downloaded folder and running `bin/karaf`.
5. Install the *netide-core* feature by first adding the feature repository file via `feature:repo-add mvn:eu.netide.core/core/1.0.0-alpha001/xml/features` and then running `feature:install netide-core`.
	- The output shold indicate that the core is waiting for the shim to connect.
6. Run the ryu-shim as indicated [here](https://github.com/fp7-netide/Engine/tree/master/ryu-shim).
	- The messages from the shim appear on the Karaf shell.
