# ODL shim client
This version of the ODL shim client is prepared to work with the Helium version of Opendaylight, and particularly using Karaf. 

# Getting OpenDaylight
First of all, we want to be able to use OpenDaylight, which already uses Openflow, and for this we will follow the second step from the following guide: https://wiki.opendaylight.org/view/OpenDaylight_OpenFlow_Plugin::Running_controller_with_the_new_OF_plugin

So clone the following repository for getting the OpenDaylight distribution:
```
git clone https://git.opendaylight.org/gerrit/p/openflowplugin.git
```

After that, go to the openflowplugin directory and run ```git checkout stable/helium``` and ```mvn clean install```. If that raises any errors, just run it adding -DskipTests (i.e. ```mvn clean install -DskipTests```). If that still raises any errors, run ```mvn dependency:tree```, which hopefully will solve all the dependencies. 

At this point, you have OpenDaylight Helium ready to be run. Now cd to /openflowplugin/distribution/karaf/target/assembly/bin/ (omit /openflowplugin/ if you were already in this folder)  and here launch Karaf by running ```./karaf```

> **Note:** Each time you recompile the ODL bundle and generate the .jar, all the contents inside data folder (in openflowplugin/distribution/karaf/target/assembly/) need to be removed. Otherwise, your bundle will automatically be installed inside karaf and you won't be able to see any of the changes. 

# Getting the ODL bundle and running ODL shim client inside Karaf
Now that you have your karaf distribution running, you will have to clone the odl-shim:
With authentication:
```git clone https://username:password@github.com/fp7-netide/Engine/```

Without authentication:
```git clone https://github.com/fp7-netide/Engine/```

Now, navigate to Engine/odl-shim and perform ```mvn clean install``` 

The .jar should be in this path:
~/.m2/repository/org/opendaylight/openflowplugin/pyretic-odl/0.1.0-SNAPSHOT
In addition, it is inside the target folder that has just being created in Engine/odl-shim. 

Go to the karaf console (which you opened before, just after running ```./bin/karaf```, right?) and install the json bundle (which is a dependency that the odl shim has) like this:
```bundle:install -s mvn:com.googlecode.json-simple/json-simple/1.1.1```

Now, install the following bundle:
```bundle:install -s mvn:org.apache.commons/commons-lang3/3.3.2```

After that, you can install the odl shim bundle just fine:
```bundle:install -s mvn:org.opendaylight.openflowplugin/pyretic-odl/0.1.0-SNAPSHOT```

You can avoid installing the json bundle if you copy .the jar (which is this one: .m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar) and paste it into openflowplugin/distribution/karaf/target/assembly/deploy. You just have to do this once. (The .m2 refers to linux platforms. If you don't know where that is, find out where maven creates it in your specific platform).

> **Note:** You have to perform the bundle:install of the odl shim each time you launch karaf. You can only put it into the deploy folder and avoid installing if it has no changes at all from the previous version. 

That should be everything. Now, when you create a new topology in mininet and ping between any of the nodes, you should be seeing things happening in the karaf console. 



