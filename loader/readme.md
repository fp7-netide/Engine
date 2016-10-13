NetIDE Application Loader
=========================

This tool validates and runs deployed NetIDE packages on client controllers.

Setup
-----

Use the script `loader/setup.sh` in the Engine repository for bootstrapping. Cleanest way to do the setup is to either put it inside a docker container or a virtual machine. The scripts assume a recent 64-Bit
Ubuntu as the OS.

Install Package
---------------

Before starting an package it needs to be installed first. To do so run the `loader/startLoader.sh` script with the install command.
Example usage: `./startLoader.sh install /path/to/some/package/Demo`. The package can either be a folder or an archive in the tar.gz format. If an archive is used it will be extracted to `/tmp/netide/`.

Available Commands
-----------------------------------

Every command is used through the `startLoader.sh`.
Currently there are the following commands available:
- `install /path/to/package/Demo` used to setup the chosen package for running.
- `run /path/to/Package/Demo --server --param` runs the given package. Server is optional, choose from ODL and ryu. Choose `ryu` for ryu, else ODL will be used. Param is optional, a parameter file can be specified with a configuration used for this run. 
- `extractionPath /path/to/extract` defines a path to which packages should be extracted.
- `extractArchive /path/to/Archive` used to extract the given archive to the extraction path. Default is /tmp/netide
- `generate /path/to/package --param` generates the param configuration specified in the file given with --param.
- `list` lists all running NetIDE controllers.
- `stop` stops all running NetIDE controllers.
- `attach` the packages are started in tmux sessions. If there are any detached sessions available this command attaches them.
