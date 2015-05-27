NetIDE Application Loader
=========================

This tool validates and runs deployed NetIDE packages on client controllers. The package format can be seen in Deliverable 3.3,
Section 3.

Setup
-----

Use the scripts in the IDE repository under `plugins/eu.netide.configuration.launcher/scripts` for bootstrapping. You need to run at
least `install_engine.sh` and an installation script for the client controller you want to use, for example `install_ryu.sh`. The
cleanest way to do the setup is to either put it inside a docker container or a virtual machine. The scripts assume a recent 64-Bit
Ubuntu as the OS.

Loading, Listing, Stopping packages
-----------------------------------

To load a package and start all applications inside it, simply `cd` to the loader directory and run `./loader.py load
<path-to-pkg>`. The loader will place runtime data such as process IDs and logs in `/tmp/netide`. The logs are in
`/tmp/netide/<UUID>/{stderr,stdout}`. The UUIDs for all running controllers can be displayed with `./loader.py list`. To stop all
running controllers, use `./loader.py stop`.
