#!/usr/bin/env python3
# coding=utf-8
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

# TODO:
# - [X] Read metadata
# - [X] Collect Apps
#   - Check parameter constraints (should always apply, but better be safe)
# - [X] Determine App->Controller mappings
# - [ ] For each app:
#   - [ ] Check system requirements
#     - [X] Hardware: CPU/RAM
#     - [ ] Installed Software: Java version? Controller software? ...?

# Package structure:
# _apps         # Network applications
#  \ _app1
#  | _app2
# _templates    # Templates for parameter and structure mapping
#  \ _template1
#  | _template2
# _system_requirements.json
# _topology_requirements.treq
# _parameters.json

import argparse
import fcntl
import inspect
import json
import logging
import os
import platform
import signal
import sys
import time
import zmq

import subprocess as sp

from loader import controllers
from loader import environment
from loader import installer
from loader import topology
from loader import util
from loader.package import Package


# TODO: store {pids,logs} somewhere in /var/{run,log}
dataroot = "/tmp/netide"
os.environ["ANSIBLE_HOST_KEY_CHECKING"] = "False"
extractPath = os.path.join(dataroot, "extractPath.txt")

logging.basicConfig(format="%(asctime)-15s %(levelname)-7s %(message)s", level=logging.DEBUG)

def extract_package(args):
    
    util.extractPackage(args.path)
    
    return 0

def set_extraction_path(args):
    util.setExtractionPath(args.path)

    return 0;

def startTest(args):
    p = Package(args.package, dataroot)
    print("controllers in package: ")
    print(p.controllers)
    for c in p.controllers_for_node().items():
        print("<--- controller in node ---->")
        print(c[1])
        c[1].startNew()


def load_package(args):
    if args.mode == "appcontroller":
        p = Package(args.package, dataroot)
        if not p.applies():
            logging.error("There's something wrong with the package")
            return 2

        os.makedirs(dataroot, exist_ok=True)
        dp = os.path.join(dataroot, "controllers.json")

        with util.FLock(open(dp, "r"), shared=True) as f:
            try:
                data  = json.load(f)
            except ValueError:
                data = {}
            if data.get("cksum", "") != p.cksum:
                logging.debug("{} != {}".format(data.get("cksum", ""), p.cksum))
                logging.error("Package changed since installation. Re-run `install' with this package.")
                # XXX
                # return 1
        try:
            pids = p.start(data.get("controllers", {}))
            logging.info(pids)
            data["controllers"] = pids
            with util.FLock(open(dp, "w")) as f:
                json.dump(data, f, indent=2)
        except Exception as err:
            logging.error(err)
            return 1
    else:

        # TODO:
        # [X] Start server controller (if not already running)
        # [ ] Make sure NetIDE stuff is running in server controller
        # [X] Start NetIDE core (if not already running)
        # [X] Connect to application controllers:
        #   [X] Copy package to remote machine
        #   [X] Run `load' with --mode=appcontroller
        # [ ] Ping core about new composition

        with util.TempDir("netide-client-dispatch") as t:

            pkg = Package(args.package, t)
            clients = pkg.get_clients()

            util.write_ansible_hosts(pkg.get_clients(), os.path.join(t, "ansible-hosts"))

            vars = {"netide_karaf_bin":
                    "{{ansible_user_dir}}/karaf/apache-karaf-4.0.0/assemblies/apache-karaf/target/assembly/bin"}
            tasks = []
            tasks.append({
                "name": "checking if Karaf (for NetIDE core) is running",
                "shell": "bash ./client -r 2 logout",
                "args": {"chdir": "{{netide_karaf_bin}}"},
                "ignore_errors": True,
                "register": "karaf_running"})
            tasks.append({
                "name": "launching karaf (for NetIDE core)",
                "shell": "bash ./start",
                "args": {"chdir": "{{netide_karaf_bin}}"},
                "when": "karaf_running.rc != 0"})

            ctasks = []
            ctasks.append({
                "shell": "mktemp -d",
                "register": "tmpdir"})
            src = os.path.join(os.getcwd(), args.package)
            ctasks.append({
                "copy": {
                    "dest": "{{tmpdir.stdout}}",
                    "src": src}})

            ctasks.append({
                "name": "loading package",
                "shell": "nohup ./netideloader.py load --mode=appcontroller {{tmpdir.stdout}}/" + str(args.package),
                "args": {"chdir": "~/netide-loader"}})

            playbook = [{"hosts": "localhost", "tasks": tasks, "vars": vars}, {"hosts": "clients", "tasks": ctasks}]
            with open(os.path.join(t, "a-playbook.yml"), "w") as ah:
                json.dump(playbook, ah, indent=2)
               
            util.spawn_logged(["ansibleEnvironment/bin/ansible-playbook", "-i", os.path.join(t, "ansible-hosts"), os.path.join(t, "a-playbook.yml")])

            # Make netip python library available. This has been installed here by installer.do_server_install()
            p = ["~", "Core", "libraries", "netip", "python"]
            sys.path.append(os.path.expanduser(os.path.join(*p)))
  
            from netip import NetIDEOps

            m = NetIDEOps.netIDE_encode(
                    NetIDEOps.NetIDE_type["NETIDE_MGMT"],
                    None,
                    None,
                    None,
                    json.dumps({"command": "update_composition", "parameters": {"composition": pkg.get_composition()}}).encode())

            with zmq.Context() as ctx, ctx.socket(zmq.REQ) as s:
                s.connect("tcp://localhost:5555")
                s.send(m)
    return 0

def list_controllers(args):
    if args.mode not in ["all", "appcontroller"]:
        logging.error("Unknown mode {}".format(args.mode))

    if args.mode == "appcontroller":
        if args.package is not None:
            logging.warning("Package argument only makes sense on the server controller")
        try:
            with util.FLock(open(os.path.join(dataroot, "controllers.json")), shared=True) as f:
                d = { platform.node(): json.load(f) }
                print(json.dumps(d, indent=2))
        except Exception as err:
            logging.error(err)
            return 1
    else:
        data = {}
        if args.package is None:
            logging.error("Package argument required")
            return 1
        with util.TempDir("netide-show") as t:
            pkg = Package(args.package, t)
            for c in pkg.get_clients():
                ssh = util.build_ssh_commands(c)
                cmd = "cd ~/netide-loader; ./netideloader.py list --mode=appcontroller"
                try:
                    data.update(json.loads(sp.check_output(ssh + [cmd], stderr=sp.DEVNULL).strip().decode('utf-8')))
                except sp.CalledProcessError as e:
                    logging.warning("Could not get list output from {}: {}".format(c[0], e))
        print(json.dumps(data, indent=2))
    return 0


def stop_controllers(args):
    if args.mode not in ["all", "appcontroller"]:
        logging.error("Unknown stop mode {}, expected one of ['all', 'appcontroller']".format(args.mode))
        return 1

    if args.mode == "appcontroller":
        if args.package is not None:
            logging.error("Package argument is only meaningful on server controllers")
            return 1
        with util.FLock(open(os.path.join(dataroot, "controllers.json"), "r+")) as f:
            try:
                d = json.load(f)
                for c in d["controllers"]:
                    for pid in [p["pid"] for p in d["controllers"][c]["procs"]]:
                        try:
                            # TODO: gentler (controller specific) way of shutting down?
                            os.kill(pid, signal.SIGTERM)
                            logging.info("Sent a SIGTERM to process {} for controller {}".format(pid, c))
                            time.sleep(5)
                            os.kill(pid, signal.SIGKILL)
                            logging.info("Sent a SIGKILL to process {} for controller {}".format(pid, c))
                        except ProcessLookupError:
                            pass
                f.seek(0)
                f.truncate()
                del d["controllers"]
                json.dump(d, f)
            except KeyError:
                logging.info("Nothing to stop")
            except Exception as err:
                logging.error(err)
                return 1
    else:
        if args.package is None:
            logging.error("Need a package to stop (for client host names, ports, ...)")
            return 1
        with util.TempDir("netide-stop") as t:
            pkg = Package(args.package, t)
            for c in pkg.get_clients():
                ssh = util.build_ssh_commands(c)
                logging.debug("SSH {}".format(ssh))

                cmd = "cd ~/netide-loader; ./netideloader.py stop --mode=appcontroller"
                util.spawn_logged(ssh + [cmd])
    return 0

def get_topology(args):
    if args.host is None:
        print(topology.get("127.0.0.1:8080"))
    else:
        print(topology.get(args.host))
    return 0

def install(args):

    logging.debug(args)
    if args.mode not in ["all", "appcontroller"]:
        logging.error("Unknown installation mode '{}'. Expected one of ['all', 'appcontroller']".format(args.mode))
        return 1
    if args.mode == "all":
        # TODO:
        # [ ] Prepare server controller
        #     [ ] Run self with arguments ['install-servercontroller', args.package]
        # [ ] Prepare application controllers
        try:
            print("server install skipped for test run")
            #installer.do_server_install(args.package)
        except installer.InstallException as e:
            logging.error("Failed to install server: {}".format(str(e)))
            return 1

        try:
            installer.do_client_installs(args.package, dataroot)
        except installer.InstallException as e:
            logging.error("Failed to install clients: {}".format(str(e)))
            return 1

    return 0

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Manage NetIDE packages")
    subparsers = parser.add_subparsers()
    
    parser_startTest = subparsers.add_parser("startTest", description="Load a NetIDE package and start its applications")
    parser_startTest.add_argument("package", type=str, help="Package to load")
    parser_startTest.add_argument("--mode", type=str, help="Loading mode, one of {appcontroller,all}")
    parser_startTest.set_defaults(func=startTest, mode="all")
    
    parser_extract = subparsers.add_parser("extractArchive", description ="extractsArchive")
    parser_extract.add_argument("path", type=str, help="Path to archive")
    parser_extract.set_defaults(func=extract_package, mode="all")
    
    parser_extract_path = subparsers.add_parser("extractionPath", description ="Set the extraction path to the given argument")
    parser_extract_path.add_argument("path", type=str, help="Path to store extracted package")
    parser_extract_path.set_defaults(func=set_extraction_path, mode="all")

    parser_load = subparsers.add_parser("load", description="Load a NetIDE package and start its applications")
    parser_load.add_argument("package", type=str, help="Package to load")
    parser_load.add_argument("--mode", type=str, help="Loading mode, one of {appcontroller,all}")
    parser_load.set_defaults(func=load_package, mode="all")

    parser_list = subparsers.add_parser("list", description="List currently running NetIDE controllers")
    parser_list.add_argument("--mode", type=str, help="List mode, one of {appcontroller,all}", default="all")
    parser_list.add_argument("package",
        type=str, nargs="?", help="Package to list controllers of (only on server controller")
    parser_list.set_defaults(func=list_controllers)

    parser_stop = subparsers.add_parser("stop", description="Stop all currently runnning NetIDE controllers")
    parser_stop.add_argument("package", type=str, nargs="?", help="Package to stop (only on server controllers)")
    parser_stop.add_argument("--mode", type=str, default="all", help="Stop mode, one of {appcontroller,all}, defaults to all")
    parser_stop.set_defaults(func=stop_controllers)

    parser_topology = subparsers.add_parser("gettopology", description="Show network topology")
    parser_topology.add_argument("host",
            type=str, help="Server controller host:port to query, defaults to 127.0.0.1:8080", nargs="?")
    parser_topology.set_defaults(func=get_topology)

    parser_install = subparsers.add_parser("install",
            description="Prepare machines listed in `package' by installing required software")
    parser_install.add_argument("--mode",
            type=str, help="Installation mode, one of {appcontroller,all}, defaults to all")
    parser_install.add_argument("package", type=str, help="Package to prepare for")
    parser_install.set_defaults(func=install, mode="all")

    args = parser.parse_args()
    if 'func' not in vars(args):
        parser.print_help()
        sys.exit(1)
    if "mode" in vars(args) and args.mode == "appcontroller":
        f = logging.Formatter("Client: %(levelname)-7s %(message)s")
        l = logging.getLogger()
        l.handlers[0].setFormatter(f)
    sys.exit(args.func(args))
