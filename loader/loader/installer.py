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

import json
import logging
import os
import platform
import requests
import stat
import subprocess as sp
import sys
import tempfile

from loader import environment
from loader import util
from loader.package import Package

class InstallException(Exception): pass

def do_server_install(pkg):
    logging.debug("Doing server install for '{}' now".format(pkg))

    prefix = os.path.expanduser("~")

    with util.TempDir("netide-server-install") as t:
        p = Package(pkg, t)
        if "server" not in p.config:
            raise InstallException('"server" section missing from configuration!')

        conf = p.config["server"]
        if "host" in conf and platform.node() != conf["host"]:
            raise InstallException("Attempted server installation on host {} (!= {})".format(platform.node(), conf["host"]))

        logging.info("Starting installation/update of NetIDE core")
        # Install NetIDE core
        if not os.path.exists(os.path.join(prefix, "Core")):
            os.makedirs(prefix, exist_ok=True)
            with util.Chdir(prefix):
                util.spawn_logged(["git", "clone", "-b", "CoreImplementation", "https://github.com/fp7-netide/Engine.git", "Core"])
        else:
            with util.Chdir(os.path.join(prefix, "Core")):
                util.spawn_logged(["git", "pull", "origin", "CoreImplementation"])

        with util.Chdir(os.path.join(prefix, "Core", "lib", "netip", "java")):
            util.spawn_logged(["mvn", "install", "-DskipTests"])

        with util.Chdir(os.path.join(prefix, "Core", "core")):
            util.spawn_logged(["mvn", "install", "-DskipTests"])

        if not os.path.exists(os.path.join(prefix, "karaf", "apache-karaf-4.0.0", "assemblies", "apache-karaf", "target", "assembly", "bin")):
            logging.info("Installing Karaf")
            os.makedirs(os.path.join(prefix, "karaf"), exist_ok=True)
            with util.Chdir(os.path.join(prefix, "karaf")):
                p = sp.Popen(["tar", "xzvf", "-"], stdin=sp.PIPE, stdout=sp.DEVNULL)
                req = requests.get("http://apache.lauf-forum.at/karaf/4.0.0/apache-karaf-4.0.0-src.tar.gz", stream=True)
                for c in req.iter_content(2048):
                    p.stdin.write(c)
                (out, err) = p.communicate()
                logging.debug("o: {}, e: {}".format(out, err))
                with util.Chdir("apache-karaf-4.0.0"):
                    util.spawn_logged(["mvn", "-Pfastinstall"])
        path = os.path.join(prefix, "karaf", "apache-karaf-4.0.0", "assemblies", "apache-karaf", "target", "assembly")
        lines = []
        with open(os.path.join(path, "etc", "org.apache.karaf.management.cfg"), "r") as fh:
            for l in fh:
                l = l.strip()
                if l.startswith("rmiRegistryPort"):
                    l = l.replace("1099", "1100")
                if l.startswith("rmiServerPort"):
                    l  = l.replace("44444", "55555")
                lines.append(l)
        with open(os.path.join(path, "etc", "org.apache.karaf.management.cfg"), "w") as fh:
            fh.write("\n".join(lines))

        with util.Chdir(os.path.join(path, "bin")):
            m = stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP | stat.S_IROTH | stat.S_IXOTH
            os.chmod("karaf", m)
            os.chmod("start", m)
            os.chmod("client", m)
            if util.spawn_logged(["bash", "./client", "-r", "2", "logout"]) == 1:
                util.spawn_logged(["bash", "./start"])
            util.spawn_logged(["bash", "./client", "-r", "10", "feature:repo-add mvn:eu.netide.core/core/1.0.0.0/xml/features"])
            util.spawn_logged(["bash", "./client", "-r", "10", "feature:install netide-core"])

        # Install server controller
        if "type" not in conf:
            raise InstallException('"type" section missing from configuration!')

        logging.debug("Attempting install of server controller {}".format(conf["type"]))

        if conf["type"] not in ["odl"]:
            raise InstallException("Don't know how to do a server controller installation for controller {}!".format(conf["type"]))

        scripts = ["~", "IDE", "plugins", "eu.netide.configuration.launcher", "scripts"]
        if not os.path.exists(os.path.join(prefix, "IDE")):
            # Repo not yet checked out
            os.makedirs(prefix, exist_ok=True)
            with util.Chdir(prefix):
                util.spawn_logged(["git", "clone", "-b", "development", "https://github.com/fp7-netide/IDE.git"])
        else:
            with util.Chdir(os.path.join(prefix, "IDE")):
                util.spawn_logged(["git", "pull", "origin", "development"])

        script = os.path.expanduser(os.path.join(*scripts + ["install_engine.sh"]))
        util.spawn_logged(["bash", script])

        script = os.path.expanduser(os.path.join(*scripts + ["install_{}.sh".format(conf["type"])]))
        logging.debug("Using script {} to install {} ({})".format(script, conf["type"], os.path.exists(script)))

        util.spawn_logged(["bash", script])


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

def do_appcontroller_install(pkg, dataroot):
    # TODO
    # For all apps on local controller:
    #   [ ] Check hardware requirements
    #   [ ] Gather required software
    #   [ ] Install required software
    #       [ ] Controllers: Use IDE install scripts
    logging.debug("Running app controller install for package {}".format(pkg))
    with util.TempDir("netide-appcontroller-install") as t:
        pkg = Package(pkg, t)
        if pkg.applies(dataroot):
            logging.info("Nothing to be done for '{}'".format(pkg))
            return

        if os.path.exists(os.path.expanduser("~/IDE")):
            # Already checked out, update
            with util.Chdir(os.path.expanduser("~/IDE")):
                logging.info("Updating NetIDE repository")
                s = sp.check_output(["git", "pull", "origin", "development"], stderr=sp.STDOUT).decode("utf-8").strip()
        else:
            logging.info("Cloning NetIDE IDE repository")
            with util.Chdir(os.path.expanduser("~")):
                s = sp.check_output(["git", "clone", "-b", "development", "https://github.com/fp7-netide/IDE.git"],
                        stderr=sp.STDOUT).decode('utf-8').strip()
        logging.debug(s)

        # Install Engine
        script = ["~", "IDE", "plugins", "eu.netide.configuration.launcher", "scripts", "install_engine.sh"]
        script = os.path.expanduser(os.path.join(*script))
        logging.debug("Installing Engine with script '{}'".format(script))
        r = util.spawn_logged(["bash", script])

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
            with util.Chdir(os.path.expanduser("~")):
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

        os.makedirs(dataroot, exist_ok=True)
        with util.FLock(open(os.path.join(dataroot, "controllers.json"), "a+")) as f:
            f.seek(0)
            try:
                data = json.load(f)
            except ValueError:
                data = {}

            data["cksum"] = pkg.cksum

            json.dump(data, f, indent=2)
            logging.debug("post-inst: {} ({})".format(dataroot, str(pkg.cksum)))
