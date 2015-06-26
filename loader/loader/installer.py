"""
 Copyright (c) 2015, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )

 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html

 Authors:
     Gregor Best, gbe@mail.upb.de
"""

import logging
import os
import shutil
import subprocess as sp
import sys
import tempfile

from loader.package import Package

class TempDir(object):
    "Context manager for temporary directories"
    d = None

    def __init__(self, prefix=""):
        self.p = prefix

    def __enter__(self):
        self.d = tempfile.mkdtemp(self.p)
        return self.d

    def __exit__(self, exc_type, exc_value, traceback):
        if self.d is None:
            return

        shutil.rmtree(self.d, ignore_errors=True)

class InstallException(Exception): pass

def do_server_install(pkg):
    logging.debug("Doing server install for '{}' now".format(pkg))
    with TempDir("netide-server-install") as t:
        p = Package(pkg, t)
        if p.applies():
            logging.info("Nothing to be done for '{}'".format(pkg))

def do_client_installs(pkg):
    "Dispatches installation requests to client machines after gaining a foothold on them. Requires passwordless SSH access to \
    client machines and passwordless root via sudo on client machines"
    # Feels like a worm...

    # TODO:
    # [ ] Monitor progress
    # [ ] Concurrent launch on multiple client machines

    # TODO: replace with list of client controllers from topology file
    # clients = ['10.0.2.15']
    clients = [ ('vagrant@127.0.0.1', '2200') ]
    for c in clients:
        if len(c) == 2:
            ssh = ["ssh", "-p", str(c[1])]
            scp = ["scp", "-P", str(c[1])]
        else:
            ssh = ["ssh"]
            scp = ["scp"]
        ssh.append(c[0])
        scp.append("-r")

        logging.info("Doing client install for '{}' on host {} now".format(pkg, c))
        # TODO:
        # [X] Copy self to application controller (SCP)
        where = sp.check_output(ssh + ["pwd"], stderr=sp.STDOUT).strip().decode('utf-8')
        where = "{}/netide-loader".format(where)

        p = sp.check_output(ssh + ["mkdir -p {}".format(where)], stderr=sp.STDOUT)
        logging.info("Copying NetIDE loader to target '{}'".format(c))
        p = sp.check_output(scp + [".", "{}:{}".format(c[0], where)], stderr=sp.STDOUT)

        # [X] Run setup.sh on app controller to bootstrap ourselves
        logging.info("Bootstrapping NetIDE loader on '{}'".format(c))
        p = sp.check_output(ssh + ["cd {}; ./setup.sh".format(where)], stderr=sp.STDOUT)

        # [X] Copy package to app controller
        dir = sp.check_output(ssh + ["mktemp", "-d"], stderr=sp.STDOUT).strip().decode('utf-8')
        logging.info("Copying NetIDE package '{}' to target '{}'".format(pkg, c))
        p = sp.check_output(scp + [pkg, "{host}:{dir}".format(host=c[0], dir=dir)], stderr=sp.STDOUT)

        # [X] Run self with arguments ['install-appcontroller', args.package]
        # [ ] Log output somewhere
        # [ ] Catch failure
        logging.info("Running NetIDE client controller installation on {}".format(c))
        p = sp.check_output(ssh + ["cd {where}; ./netideloader.py install --mode appcontroller {pkg}".format(where=where, pkg=os.path.join(dir, pkg))],
                stderr=sp.STDOUT)
        logging.debug(p.decode('utf-8'))

        # [X] Remove package from app controller
        p = sp.check_output(ssh + ["rm -r {}".format(dir)], stderr=sp.STDOUT).strip().decode('utf-8')

def do_appcontroller_install(pkg):
    # TODO
    # For all apps on local controller:
    #   [ ] Check hardware requirements
    #   [ ] Gather required software
    #   [ ] Install required software
    #       [ ] Controllers: Use IDE install scripts
    logging.debug("Running app controller install for package {}".format(pkg))
    with TempDir("netide-appcontroller-install") as t:
        pkg = Package(pkg, t)
        if pkg.applies():
            logging.info("Nothing to be done for '{}'".format(pkg))
            return

        if os.path.exists(os.path.expanduser("~/IDE")):
            # Already checked out, update
            os.chdir(os.path.expanduser("~/IDE"))
            logging.info("Updating NetIDE repository")
            # s = sp.check_output(["git", "pull", "origin", "development"], stderr=sp.STDOUT).decode("utf-8").strip()
            s = "skipped"
        else:
            logging.info("Cloning NetIDE IDE repository")
            os.chdir(os.path.expanduser("~"))
            s = sp.check_output(["git", "clone", "-b", "development", "https://github.com/fp7-netide/IDE.git"],
                    stderr=sp.STDOUT).decode('utf-8').strip()
        logging.debug(s)

        # The goodies are now in ~/IDE/plugins/eu.netide.configuration.launcher/scripts/
        for c in pkg.controllers:
            cname = c.__name__.lower()
            logging.debug("Handling controller {}".format(cname))
            if cname not in ["ryu", "floodlight", "odl", "pox", "pyretic"]:
                raise InstallException("Don't know how to install controller {}".format(cname))

            script = ["~", "IDE", "plugins", "eu.netide.configuration.launcher", "scripts"]
            script.append("install_{}.sh".format(cname))
            script = os.path.expanduser(os.path.join(*script))

            logging.debug("Using script {} ({})".format(script, os.path.exists(script)))
            p = sp.Popen(["bash", script], stdout=sp.PIPE, stderr=sp.STDOUT)
            for l in p.stdout:
                l = l.decode('utf-8').rstrip()
                logging.debug(l)
            r = p.wait()
            logging.debug("Finished installing controller {} with exit code {}".format(cname, r))

        # TODO:
        # [ ] Install languages
        # [ ] Install libraries
