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

# XXX make this configurable
install_package_command = "apt-get install --yes {}"

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


def write_ansible_hosts(clients, path):
    # Tuple layout:
    # (name, ssh host, ssh port, ssh identity)
    with open(path, "w") as ah:
        ah.write("localhost ansible_connection=local\n")
        ah.write("\n[clients]\n")
        for c in clients:
            try:
                user, host = c[1].split("@", 1)
                ah.write("{} ansible_ssh_host={} ansible_ssh_user={}".format(c[0], host, user))
            except ValueError:
                ah.write("{} ansible_ssh_host={}".format(c[0], c[1]))
            if len(c) >= 3:
                ah.write(" ansible_ssh_port={}".format(c[2]))
            if len(c) >= 4:
                ah.write(" ansible_ssh_private_key_file={}".format(c[3]))
            ah.write("\n")


def do_client_installs(pkgpath, dataroot):
    "Dispatches installation requests to client machines after gaining a foothold on them. Requires passwordless SSH access to \
    client machines and passwordless root via sudo on client machines"
    # Feels like a worm...

    # TODO:
    # [ ] Monitor progress
    # [ ] Concurrent launch on multiple client machines

    with util.TempDir("netide-client-installs") as t:
        pkg = Package(pkgpath, t)
        clients = pkg.get_clients()

        write_ansible_hosts(clients, os.path.join(t, "ansible-hosts"))

        tasks = []

        tasks.append({
            "name": "Install NetIDE loader",
            "synchronize": {
                "dest": "~/netide-loader",
                "src" : os.getcwd() + "/"}})

        tasks.append({
            "name": "Bootstrap NetIDE loader",
            "shell": "./setup.sh",
            "args": { "chdir": "~/netide-loader" }})

        # XXX: update repo if it already exists
        tasks.append({
            "name": "Clone IDE repository",
            "shell": "git clone -b development http://github.com/fp7-netide/IDE.git",
            "args": {
                "creates": "~/IDE",
                "chdir": "~" }})

        tasks.append({
            "name": "Install Engine",
            "shell": "bash ~/IDE/plugins/eu.netide.configuration.launcher/scripts/install_engine.sh"})

        tasks.append({
            "name": "Create {}".format(dataroot),
            "file": {
                "path": dataroot,
                "state": "directory"}})

        tasks.append({
            "name": "Register Package checksum",
            "copy": {
                "content": json.dumps({"cksum": pkg.cksum}, indent=2),
                "dest": os.path.join(dataroot, "controllers.json")}})

        playbook = [{"hosts": "clients", "tasks": tasks}]

        logging.debug("Client list for {}: {}".format(pkgpath, clients))
        for c in clients:
            ctasks = []
            ssh, _ = util.build_ssh_commands(c)

            logging.debug("SSH: {}".format(ssh))

            # Install Python2.7 so we can use ansible
            # XXX: make the package name configurable
            # XXX: use ansible -m raw instead of plain ssh
            util.spawn_logged(ssh + ["sudo", install_package_command.format("python2.7")])

            apps = []
            # Collect controllers per client machine and collect applications
            for con in pkg.controllers_for_node(c[0]):
                apps.extend(con.applications)
                cname = con.__name__.lower()
                logging.debug("Adding controller {} to ansible playbook".format(cname))
                if cname not in ["ryu", "floodlight", "odl", "pox", "pyretic"]:
                    raise InstallException("Don't know how to install controller {}".format(cname))

                script = ["~", "IDE", "plugins", "eu.netide.configuration.launcher", "scripts"]
                script.append("install_{}.sh".format(cname))

                ctasks.append({
                    "name": "install {}".format(cname),
                    "shell": "bash {}".format(os.path.join(*script)),
                    "args": {"chdir": "~"}})

            # Install application dependencies
            # XXX: ugly :/
            # XXX: libraries
            for a in apps:
                reqs = a.metadata.get("requirements", {}).get("Software", {})

                # Languages
                for l in reqs.get("Languages", {}):
                    if l["name"] == "python":
                        if l["version"].startswith("3"):
                            l["name"] += "3"
                        else:
                            l["name"] += "2"
                    elif l["name"] == "java":
                        if "7" in l["version"]:
                            l["name"] = "openjdk-7-jdk"
                        elif "8" in l["version"]:
                            l["name"] = "openjdk-8-jdk"
                        else:
                            l["name"] = "openjdk-6-jdk"

                    ctasks.append({
                        "name": "install {} (for app {})".format(l["name"], str(a)),
                        "apt": {"pkg": "{}={}*".format(l["name"], l["version"])}})
            playbook.append({"hosts": c[0], "tasks": ctasks})

        # A valid JSON-document is also valid YAML, so we can take a small shortcut here
        with open(os.path.join(t, "a-playbook.yml"), "w") as ah:
            json.dump(playbook, ah, indent=2)
        util.spawn_logged(["env", "ANSIBLE_HOST_KEY_CHECKING=False", "ansible-playbook", "-i", os.path.join(t, "ansible-hosts"), os.path.join(t, "a-playbook.yml")])
