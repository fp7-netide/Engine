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

from subprocess import call
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
        if not p.load_apps_and_controller():
            logging.error("There's something wrong with the package")
            return 2

        call(["./virtualEnv_Ansible_Install.sh"])


        if "server" not in p.config:
            raise InstallException('"server" section missing from configuration!')

        conf = p.config["server"]
        util.editPlaybookServer(conf)
        if "host" in conf and platform.node() != conf["host"] and conf["host"] != "localhost":
            raise InstallException("Attempted server installation on host {} (!= {})".format(platform.node(), conf["host"]))

       # with open("Playbook_Setup/sever.yml", "w") as serverYml:
        #    serverYml.write("--- \n - name: install prereq for all hosts \n hosts: localhost \n roles: - prereq - core \n ...")

        #install core and engine on server (usually localhost)
        #read p.config[server] and add server to site.yml
        call(["ansibleEnvironment/bin/ansible-playbook", "-v", os.path.join("Playbook_Setup", "siteServer.yml")])


def do_client_installs(pkgpath, dataroot):
    "Dispatches installation requests to client machines after gaining a foothold on them. Requires passwordless SSH access to \
    client machines and passwordless root via sudo on client machines"

    with util.TempDir("netide-client-installs") as t:
        pkg = Package(pkgpath, t)
        if not pkg.load_apps_and_controller():
            logging.error("There's something wrong with the package")
            return 2
        clients = pkg.get_clients()
        #controller = pkg.controllers
        #print("controller: ")
        #print(controller)
        #for n in controller:
        #    print("instance of controller: ")
        #    print(n)
        #    for i in controller[n]:
        #        print(i)

        util.editPlaybookClient(pkg)
        util.spawn_logged(["ansibleEnvironment/bin/ansible-playbook", "-v", os.path.join("Playbook_Setup", "siteClient.yml")])

#===============================================================================
#         util.write_ansible_hosts(clients, os.path.join(t, "ansible-hosts"))
#
#         tasks = []
#
#         # Can't use `synchronize' here because that doesn't play nice with ssh options
#         tasks.append({
#             "name": "Copy NetIDE loader",
#             "copy": {
#                 "dest": '{{ansible_user_dir}}/netide-loader-tmp',
#                 "src" : os.getcwd()}})
#
#         # We need to do this dance because `copy' copies to a subdir unless
#         # `src' ends with a '/', in which case it doesn't work at all (tries
#         # to write to '/' instead)
#         tasks.append({
#             "shell": "mv {{ansible_user_dir}}/netide-loader-tmp/loader {{ansible_user_dir}}/netide-loader",
#             "args": {"creates": "{{ansible_user_dir}}/netide-loader"}})
#         tasks.append({"file": {"path": "{{ansible_user_dir}}/netide-loader-tmp", "state": "absent"}})
#         tasks.append({"file": {"path": "{{ansible_user_dir}}/netide-loader/netideloader.py", "mode": "ugo+rx"}})
#
#         tasks.append({
#             "name": "Bootstrap NetIDE loader",
#             "shell": "bash ./setup.sh",
#             "args": { "chdir": "{{ansible_user_dir}}/netide-loader" }})
#
#         #is already cloned...
#         tasks.append({
#             "name": "Clone IDE repository",
#             "git": {
#                 "repo": "http://github.com/fp7-netide/IDE.git",
#                 "dest": "{{ansible_user_dir}}/IDE",
#                 "version": "development"}})
#
#         #has been done in setup server
#         tasks.append({
#             "name": "Install Engine",
#             "shell": "bash {{ansible_user_dir}}/IDE/plugins/eu.netide.configuration.launcher/scripts/install_engine.sh"})
# #add creates:
#         tasks.append({
#             "file": {
#                 "path": dataroot,
#                 "state": "directory"}})
#
#         tasks.append({
#             "name": "Register Package checksum",
#             "copy": {
#                 "content": json.dumps({"cksum": pkg.cksum}, indent=2),
#                 "dest": os.path.join(dataroot, "controllers.json")}})
#
#         playbook = [{"hosts": "clients", "tasks": tasks}]
#
#         #use new role system here !
#         for c in clients:
#
#             ctasks = []
#
#             apps = []
#             # Collect controllers per client machine and collect applications
#             for con in pkg.controllers_for_node(c[0]):
#                 apps.extend(con.applications)
#                 cname = con.__name__.lower()
#                 if cname not in ["ryu", "floodlight", "odl", "pox", "pyretic"]:
#                     raise InstallException("Don't know how to install controller {}".format(cname))
#
#                 script = ["{{ansible_user_dir}}", "IDE", "plugins", "eu.netide.configuration.launcher", "scripts"]
#                 script.append("install_{}.sh".format(cname))
#
#                 ctasks.append({
#                     "name": "install controller {}".format(cname),
#                     "shell": "bash {}".format(os.path.join(*script)),
#                     "args": {"chdir": "{{ansible_user_dir}}"}})
#
#             # Install application dependencies
#             # XXX: ugly :/
#             # XXX: libraries
#             for a in apps:
#                 reqs = a.metadata.get("requirements", {}).get("Software", {})
#
#                 # Languages
#                 for l in reqs.get("Languages", {}):
#                     if l["name"] == "python":
#                         if l["version"].startswith("3"):
#                             l["name"] += "3"
#                         else:
#                             l["name"] += "2"
#                     elif l["name"] == "java":
#                         if "7" in l["version"]:
#                             l["name"] = "openjdk-7-jdk"
#                         elif "8" in l["version"]:
#                             l["name"] = "openjdk-8-jdk"
#                         else:
#                             l["name"] = "openjdk-6-jdk"
#
#                     ctasks.append({
#                         "name": "install {} (for app {})".format(l["name"], str(a)),
#                         "apt": {"pkg": "{}={}*".format(l["name"], l["version"])}})
#             playbook.append({"hosts": c[0], "tasks": ctasks})
#
#         # A valid JSON-document is also valid YAML, so we can take a small shortcut here
#         with open(os.path.join(t, "a-playbook.yml"), "w") as ah:
#             json.dump(playbook, ah, indent=2)
#             print(playbook)
#         util.spawn_logged(["ansibleEnvironment/bin/ansible-playbook", "-v", "-i", os.path.join(t, "ansible-hosts"), os.path.join(t, "a-playbook.yml")])
#===============================================================================
