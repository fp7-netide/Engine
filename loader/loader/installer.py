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
install_package_command = "sudo apt-get install --yes {}"

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

        tasks = []

        tasks.append({
            "name": "Clone NetIDE Engine (master)",
            "git": {
                "repo": "https://github.com/fp7-netide/Engine.git",
                "dest": "{{ansible_user_dir}}/Engine"}})
        tasks.append({
            "name": "Clone NetIDE Engine (Core)",
            "git": {
                "repo": "https://github.com/fp7-netide/Engine.git",
                "dest": "{{ansible_user_dir}}/Core",
                "version": "CoreImplementation"}})
        tasks.append({
            "name": "Clone NetIDE IDE",
            "git": {
                "repo": "https://github.com/fp7-netide/IDE.git",
                "dest": "{{ansible_user_dir}}/IDE"}})

        tasks.append({
            "name": "Build NetIDE netip library (java)",
            "shell": "mvn install -Dskiptests",
            "args": {
                "chdir": "{{ansible_user_dir}}/Engine/libraries/netip/java",
                "creates": "{{ansible_user_dir}}/Engine/libraries/netip/java/target/netip-1.0.0.0.jar"}})
        tasks.append({
            "name": "Build NetIDE Core",
            "shell": "mvn install -DskipTests",
            "args": {
                "chdir": "{{ansible_user_dir}}/Core/core",
                "creates": "{{ansible_user_dir}}/Core/core/core.caos/target/core.caos-1.0.0.0.jar"}})

        tasks.append({"file": {"path": "{{ansible_user_dir}}/karaf", "state": "directory"}})
        tasks.append({
            "name": "Downloading Karaf",
            "unarchive": {
                "copy": "no",
                "creates": "{{ansible_user_dir}}/karaf/apache-karaf-4.0.0",
                "dest": "{{ansible_user_dir}}/karaf",
                "src": "http://apache.lauf-forum.at/karaf/4.0.0/apache-karaf-4.0.0-src.tar.gz"}})
        tasks.append({
            "name": "Building and installing Karaf",
            "shell": "mvn -Pfastinstall",
            "args": {
                "chdir": "{{ansible_user_dir}}/karaf/apache-karaf-4.0.0",
                "creates": "{{netide_karaf_assembly}}/bin"}})
        # This avoids conflicts with ODL's Karaf instance
        tasks.append({
            "name": "Reconfiguring Karaf RMI ports (1/2)",
            "lineinfile": {
                "dest": "{{netide_karaf_assembly}}/etc/org.apache.karaf.management.cfg",
                "regexp": "^rmiRegistryPort.*1099",
                "line": "rmiRegistryPort = 1100"}})
        tasks.append({
            "name": "Reconfiguring Karaf RMI ports (2/2)",
            "lineinfile": {
                "dest": "{{netide_karaf_assembly}}/etc/org.apache.karaf.management.cfg",
                "regexp": "^rmiServerPort.*44444",
                "line": "rmiServerPort = 55555"}})
        tasks.append({ "file": {"path": "{{netide_karaf_assembly}}/bin/karaf", "mode": "ugo+rx"}})
        tasks.append({ "file": {"path": "{{netide_karaf_assembly}}/bin/start", "mode": "ugo+rx"}})
        tasks.append({ "file": {"path": "{{netide_karaf_assembly}}/bin/client", "mode": "ugo+rx"}})
        tasks.append({
            "name": "Checking of Karaf is running",
            "shell": "bash ./client -r 2 logout",
            "args": {"chdir": "{{netide_karaf_assembly}}/bin"},
            "ignore_errors": True,
            "register": "karaf_running"})
        tasks.append({
            "name": "Starting Karaf",
            "shell": "bash ./start",
            "when": "karaf_running.rc != 0",
            "args": { "chdir": "{{netide_karaf_assembly}}/bin" }})
        tasks.append({
            "name": "Adding NetIDE Maven Repo to Karaf",
            "shell": 'bash ./client -r 10 "feature:repo-add mvn:eu.netide.core/core/1.0.0.0/xml/features"',
            "args": { "chdir": "{{netide_karaf_assembly}}/bin" }})
        tasks.append({
            "name": "Installing NetIDE Core Karaf feature",
            "shell": 'bash ./client -r 10 "feature:install netide-core"',
            "args": { "chdir": "{{netide_karaf_assembly}}/bin"}})

        tasks.append({
            "name": "Installing NetIDE Engine",
            "shell": "bash {{netide_scripts}}/install_engine.sh"})

        if conf["type"] not in ["odl"]:
            raise InstallException("Don't know how to do a server controller installation for controller {}!".format(conf["type"]))
        tasks.append({
            "name": "Installing {}".format(conf["type"]),
            "shell": "bash {{netide_scripts}}/install_{}.sh".format(conf["type"])})

        with open(os.path.join(t, "a-playbook.yml"), "w") as fh:
            json.dump([{
                "hosts": "localhost",
                "tasks": tasks,
                "vars": {
                    "netide_karaf_assembly":
                        "{{ansible_user_dir}}/karaf/apache-karaf-4.0.0/assemblies/apache-karaf/target/assembly",
                    "netide_scripts": "{{ansible_user_dir}}/IDE/plugins/eu.netide.configuration.launcher/scripts" }}], fh)
        util.write_ansible_hosts([], os.path.join(t, "a-hosts"))
        util.spawn_logged(["ansible-playbook", "-i", os.path.join(t, "a-hosts"), os.path.join(t, "a-playbook.yml")])


def do_client_installs(pkgpath, dataroot):
    "Dispatches installation requests to client machines after gaining a foothold on them. Requires passwordless SSH access to \
    client machines and passwordless root via sudo on client machines"

    with util.TempDir("netide-client-installs") as t:
        pkg = Package(pkgpath, t)
        clients = pkg.get_clients()

        util.write_ansible_hosts(clients, os.path.join(t, "ansible-hosts"))

        # Install Python2.7 so we can use ansible
        # XXX: make the package name configurable
        util.spawn_logged(["ansible", "clients", "-i", os.path.join(t, "ansible-hosts"),
            "-m", "raw", "-a", install_package_command.format("python2.7")])

        tasks = []

        # Can't use `synchronize' here because that doesn't play nice with ssh options
        tasks.append({
            "name": "Copy NetIDE loader",
            "copy": {
                "dest": '{{ansible_user_dir}}/netide-loader-tmp',
                "src" : os.getcwd()}})

        # We need to do this dance because `copy' copies to a subdir unless
        # `src' ends with a '/', in which case it doesn't work at all (tries
        # to write to '/' instead)
        tasks.append({
            "shell": "mv {{ansible_user_dir}}/netide-loader-tmp/loader {{ansible_user_dir}}/netide-loader",
            "args": {"creates": "{{ansible_user_dir}}/netide-loader"}})
        tasks.append({"file": {"path": "{{ansible_user_dir}}/netide-loader-tmp", "state": "absent"}})
        tasks.append({"file": {"path": "{{ansible_user_dir}}/netide-loader/netideloader.py", "mode": "ugo+rx"}})

        tasks.append({
            "name": "Bootstrap NetIDE loader",
            "shell": "bash ./setup.sh",
            "args": { "chdir": "{{ansible_user_dir}}/netide-loader" }})

        tasks.append({
            "name": "Clone IDE repository",
            "git": {
                "repo": "http://github.com/fp7-netide/IDE.git",
                "dest": "{{ansible_user_dir}}/IDE"}})

        tasks.append({
            "name": "Install Engine",
            "shell": "bash {{ansible_user_dir}}/IDE/plugins/eu.netide.configuration.launcher/scripts/install_engine.sh"})

        tasks.append({
            "file": {
                "path": dataroot,
                "state": "directory"}})

        tasks.append({
            "name": "Register Package checksum",
            "copy": {
                "content": json.dumps({"cksum": pkg.cksum}, indent=2),
                "dest": os.path.join(dataroot, "controllers.json")}})

        playbook = [{"hosts": "clients", "tasks": tasks}]

        for c in clients:
            ctasks = []

            apps = []
            # Collect controllers per client machine and collect applications
            for con in pkg.controllers_for_node(c[0]):
                apps.extend(con.applications)
                cname = con.__name__.lower()
                if cname not in ["ryu", "floodlight", "odl", "pox", "pyretic"]:
                    raise InstallException("Don't know how to install controller {}".format(cname))

                script = ["{{ansible_user_dir}}", "IDE", "plugins", "eu.netide.configuration.launcher", "scripts"]
                script.append("install_{}.sh".format(cname))

                ctasks.append({
                    "name": "install controller {}".format(cname),
                    "shell": "bash {}".format(os.path.join(*script)),
                    "args": {"chdir": "{{ansible_user_dir}}"}})

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
        util.spawn_logged(["ansible-playbook", "-i", os.path.join(t, "ansible-hosts"), os.path.join(t, "a-playbook.yml")])
