This readme helps the user to run Floodlight Backend module that connects to the OpenDaylight Shim layer

Import the code into Floodlight
-------------------------------------------------------------------------

When developing with Floodlight, you must download the Floodlight source code and add your module as a new namespace.
However, to allow you to develop independently, a POM is included that references the Floodlight project from 
within this project. You will still need to copy the source code into the Floodlight project to run it!

Setup Eclipse:

1) Download Floodlight (version 0.90) from their Github repo: https://github.com/floodlight/floodlight.git

2) Import the project into Eclipse

Import NetIDE code
3) Import the fl-odl project into Eclipse (ie, this project) 

4) Copy the "net.floodlightcontroller.interceptor" namespace to the Floodlight project including all the .java files from this namespace.

Edit Config Files:

4) A vanilla version of the Floodlight configuration files has been provided with the relevant modifications required to run this prototype.
   However, you may have additional settings configured in Floodlight that you don't want to lose. So you need to merge our settings into yours: 

Modify the floodlight properties files:

	- META-INF
		- <floodlight_folder>/src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
		- add the line: "net.floodlightcontroller.interceptor.NetideModule" at the end of Module section, before the port setting
		
	- floodlightdefault.properties
		- <floodlight_folder>/src/main/resources/floodlightdefault.properties
		- add the line: "net.floodlightcontroller.interceptor.NetideModule" at the end of the file
		- Change the port number to 7733 (we don't want the switches connecting)

5) Compile the code:
    - ANT
		- Out of the box, Floodlight uses ant as its build tool. The NetIDE code uses one additional dependency which you need to add to he build.xml file
		- You can copy the build.xml from this folder overwriting the Floodlight one or 
		- Add the following entry under <patternset id="lib"> entry: 
			<include name="json-20140107.jar"/>
		- you can now build by issuing the ant command within a shell/command window
	  
	- MAVEN
		- I have also provided a POM file so that Maven can be used to build
		- copy the floodlight-pom.xml file into the root of the floodlight project and rename to "pom.xml"
		- Now you can issue the command: mvn clean install
		- the existing pom.xml in this directory will only build this project, allowing you to develop outside of the Floodlight project
	
6) Run the jar

	- java -jar <floodlight_folder>/target/floodlight.jar


Test Steps:

1) download the source code of POX (https://github.com/noxrepo/pox.git)

2) copy the pox_client.py to pox/ext (this is a modified version without Pyretic dependencies)

3) enter the pox folder and run "python ./pox.py pox_client"

4) Start your mininet config.

You should now be able to run your floodlight application/module on top of the POX SDN controller. 
