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
import subprocess as sp
import sys

from loader import environment
from loader import util
from loader.package import Package

class InstallException(Exception): pass

def do_server_install(pkg):
    logging.debug("Doing server install for '{}' now".format(pkg))
    with util.TempDir("netide-server-install") as t:
        p = Package(pkg, t)
        if p.applies():
            logging.info("Nothing to be done for '{}'".format(pkg))

def do_client_installs(pkgpath):
    "Dispatches installation requests to client machines after gaining a foothold on them. Requires passwordless SSH access to \
    client machines and passwordless root via sudo on client machines"
    # Feels like a worm...

    # TODO:
    # [ ] Monitor progress
    # [ ] Concurrent launch on multiple client machines

    with util.TempDir("netide-client-installs") as t:
        pkg = Package(pkgpath, t)
        clients = pkg.get_clients()

        logging.debug("Client list for {}: {}".format(pkgpath, clients))
        for c in clients:
            ssh, scp = util.build_ssh_commands(c)

            logging.debug("SSH: {}".format(ssh))
            logging.debug("SCP: {}".format(scp))
            logging.info("Doing client install for '{}' on host {} now".format(pkgpath, c))
            # TODO:
            # [X] Copy self to application controller (SCP)
            where = sp.check_output(ssh + ["pwd"], stderr=sp.DEVNULL).strip().decode('utf-8')
            where = "{}/netide-loader".format(where)

            p = sp.check_output(ssh + ["mkdir -p {}".format(where)], stderr=sp.DEVNULL)
            logging.info("Copying NetIDE loader to target '{}'".format(c))
            util.spawn_logged(scp + [".", "{}:{}".format(c[0], where)])

            # [X] Run setup.sh on app controller to bootstrap ourselves
            logging.info("Bootstrapping NetIDE loader on '{}'".format(c))
            util.spawn_logged(ssh + ["cd {}; ./setup.sh".format(where)])

            # [X] Copy package to app controller
            dir = sp.check_output(ssh + ["mktemp", "-d"], stderr=sp.DEVNULL).strip().decode('utf-8')
            logging.info("Copying NetIDE package '{}' to target '{}'".format(pkgpath, c))
            util.spawn_logged(scp + [pkgpath, "{host}:{dir}".format(host=c[0], dir=dir)])

            # [X] Run self with arguments ['install-appcontroller', args.package]
            # [ ] Log output somewhere
            # [ ] Catch failure
            logging.info("Running NetIDE client controller installation on {}".format(c))
            util.spawn_logged(ssh + ["cd {where}; ./netideloader.py install --mode appcontroller {pkg}".format(where=where,
                pkg=os.path.join(dir, pkgpath))])

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
    with util.TempDir("netide-appcontroller-install") as t:
        pkg = Package(pkg, t)
        if pkg.applies():
            logging.info("Nothing to be done for '{}'".format(pkg))
            return

        if os.path.exists(os.path.expanduser("~/IDE")):
            # Already checked out, update
            os.chdir(os.path.expanduser("~/IDE"))
            logging.info("Updating NetIDE repository")
            s = sp.check_output(["git", "pull", "origin", "development"], stderr=sp.STDOUT).decode("utf-8").strip()
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
            r = util.spawn_logged(["bash", script])
            logging.debug("Finished installing controller {} with exit code {}".format(cname, r))

        # TODO:
        # [ ] Install languages
        # [ ] Install libraries
        apps = []
        for c in pkg.controllers:
            apps.extend(c.applications)
        for a in apps:
            logging.debug("Handling deps for application {}".format(a))
            reqs = a.metadata.get("requirements", {}).get("Software", {})

            # Languages
            try:
                environment.check_languages(reqs.get("Languages", {}))
            except environment.LanguageCheckException as e:
                if e.dunno:
                    raise e
                logging.debug("Would attempt to install {what} version {want} now".format(what=e.what, want=e.want))
                if e.what not in ["java", "python"]:
                    raise Exception("Don't know how to install {what} version {want}".format(what=e.what, want=e.want))
                if e.what == "python":
                    # TODO: use an external script?
                    r = util.spawn_logged(["sudo", "apt-get", "install", "--yes", "{}{}".format(e.what, e.want)])
                else:
                    version = e.want.split(".", 2)[1]
                    logging.debug("Installing java {} now".format(version))
                    r = util.spawn_logged(["sudo", "apt-get", "install", "--yes", "openjdk-{}-jdk".format(version)])

        # Sanity check
        if not pkg.applies():
            logging.error("Failed to resolve dependencies for {}".format(pkg))
