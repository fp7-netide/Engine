This readme helps the user to run Floodlight Backend module that connects to the OpenDaylight Shim layer

Import the code into Floodlight
-------------------------------------------------------------------------

When developing with Floodlight, you must download the Floodlight source code and add your module as a new namespace.
However, to allow you to develop independently, a POM is included that references the Floodlight project from 
within this project.

Setup Eclipse:
1) Download Floodlight (version 0.90) from their Github repo: https://github.com/floodlight/floodlight.git
2) Import the project into Eclipse

Import NetIDE code
3) Create the "net.floodlightcontroller.interceptor" namespace and copy the .java files into this namespace.

Edit Config Files:
4) Modify the floodlight properties files:
	- META-INF
		- <floodlight_folder>/src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
		- add the line: "net.floodlightcontroller.interceptor.NetideModule" at the end of Module section, before the port setting
	- floodlightdefault.properties
		- <floodlight_folder>/src/main/resources/floodlightdefault.properties
		- add the line: "net.floodlightcontroller.NetideModule" at the end of the file
		- Change the port number to 6634 (we don't want the switches connecting)

Compile the code:
	- Floodlight uses ant as its build tool, you can build by issuing the ant command within a shell/command window
	- I have provided a POM file so that Maven can be used: copy the floodlight-pom.xml file into the root of the 
	      floodlight project and rename to "pom.xml". Now you can issue the command "mvn clean install"
5) Run the jar
	- java -jar <floodlight_folder>/target/floodlight.jar


Test Steps:
1 download the source code of POX (https://github.com/noxrepo/pox.git)
2 copy the pox_client.py to pox/ext (this is a modified version without Pyretic dependencies)
3 enter the pox folder and run "python ./pox.py pox_client"
4 Start your mininet config.

You should now be able to run your floodlight application/module on top of the POX SDN controller. 
