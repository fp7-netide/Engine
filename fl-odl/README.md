This readme helps the user to run Floodlight Backend module that connects to the OpenDaylight Shim layer

Import the code into Floodlight
-------------------------------------------------------------------------

When developing with Floodlight, you must download the Floodlight source code and add your module as a new namespace.
There doesn't seem to be a way to develop independently and reference the Floodlight project from within your own.

Setup Eclipse:

1) Download Floodlight (version 0.90) from their Github repo: https://github.com/floodlight/floodlight.git

2) Import the project into Eclipse

Import NetIDE code

3) Copy the net.floodlightcontroller.interceptor namespace (and *.java files) into the Floodlight project. 

4) Copy the java classes (ElementsConfigurator.java and IElementsConfiguratorService.java) in the new folder

Edit Config Files:

5) Modify the floodlight properties files:

	- META-INF
		- <floodlight_folder>/src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
		- add the line: "net.floodlightcontroller.interceptor.NetideModule" at the end of Module section, before the port setting
	- floodlightdefault.properties
		- <floodlight_folder>/src/main/resources/floodlightdefault.properties
		- add the line: "net.floodlightcontroller.interceptor.NetideModule" and  "net.floodlightcontroller.interceptor.BackendChannel" at the end of the file

Compile the code:

	- Floodlight uses ant as its build tool, you can build by issuing the ant command within a shell/command window
	- I have provided a POM file so that Maven can be used, mvn clean install

5) Run the jar
	- java -jar <floodlight_folder>/target/floodlight.jar

