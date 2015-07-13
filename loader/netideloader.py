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
import signal
import sys
import time

import subprocess as sp

from loader import controllers
from loader import environment
from loader import installer
from loader import topology
from loader import util
from loader.package import Package

# TODO: store {pids,logs} somewhere in /var/{run,log}
dataroot = "/tmp/netide"

logging.basicConfig(format="%(asctime)-15s %(levelname)-7s %(message)s", level=logging.DEBUG)

class FLock(object):
    "Context manager for locking file objects with flock"
    def __init__(self, f, t=fcntl.LOCK_EX):
        self.f = f
        self.t = t

    def __enter__(self):
        fcntl.flock(self.f, self.t)
        return self.f

    def __exit__(self, exc_type, exc_value, traceback):
        fcntl.flock(self.f, fcntl.LOCK_UN)

def load_package(args):
    if args.mode == "appcontroller":
        p = Package(args.package, dataroot)
        if not p.applies():
            logging.error("There's something wrong with the package")
            return 2

        os.makedirs(dataroot, exist_ok=True)

        with FLock(open(os.path.join(dataroot, "controllers.json"), "w+")) as f:
            try:
                data  = json.load(f)
            except ValueError:
                data = {}
            f.seek(0)
            f.truncate()
            try:
                pids = p.start()
                logging.info(pids)
                data["controllers"] = pids
                json.dump(data, f, indent=2)
            except Exception as err:
                logging.error(err)
                return 1
    else:
        # TODO:
        # [ ] Start server controller (if not already running)
        # [X] Connect to application controllers:
        #   [X] Copy package to remote machine
        #   [X] Run `load' with --mode=appcontroller
        with util.TempDir("netide-client-dispatch") as t:
            pkg = Package(args.package, t)
            for c in pkg.get_clients():
                ssh, scp = util.build_ssh_commands(c)
                logging.debug("SSH {}".format(ssh))
                logging.debug("SCP {}".format(scp))
                logging.debug(c)

                dir = sp.check_output(ssh + ["mktemp", "-d"], stderr=sp.DEVNULL).strip().decode('utf-8')
                logging.debug("Temp: {}".format(dir))
                util.spawn_logged(scp + [args.package, "{host}:{dir}".format(host=c[0], dir=dir)])

                cmd = "cd ~/netide-loader; ./netideloader.py load --mode=appcontroller {}"
                if os.path.isfile(args.package): # package in a zip file
                    cmd = cmd.format(os.path.join(dir, args.package))
                else:
                    cmd = cmd.format(dir)
                logging.debug("cmd: {}".format(cmd))
                util.spawn_logged(ssh + [cmd])
    return 0

def list_controllers(args):
    if args.mode not in ["all", "appcontroller"]:
        logging.error("Unknown mode {}".format(args.mode))

    if args.mode == "appcontroller":
        if args.package is not None:
            logging.warning("Package argument only makes sense on the server controller")
        try:
            with FLock(open(os.path.join(dataroot, "controllers.json")), fcntl.LOCK_SH) as f:
                print(f.read())
        except Exception as err:
            logging.error(err)
            return 1
    else:
        if args.package is None:
            logging.error("Package argument required")
            return 1
        with util.TempDir("netide-show") as t:
            for c in Package(args.package, t).get_clients():
                ssh, _ = util.build_ssh_commands(c)
                cmd = "cd ~/netide-loader; ./netideloader.py list --mode=appcontroller"
                try:
                    data = sp.check_output(ssh + [cmd], stderr=sp.DEVNULL).strip().decode('utf-8')
                except sp.CalledProcessError:
                    logging.warning("Could not get list output from {}".format(c[0]))
                else:
                    print(data)
    return 0


def stop_controllers(args):
    if args.mode not in ["all", "appcontroller"]:
        logging.error("Unknown stop mode {}, expected one of ['all', 'appcontroller']".format(args.mode))
        return 1

    if args.mode == "appcontroller":
        if args.package is not None:
            logging.error("Package argument is only meaningful on server controllers")
            return 1
        with FLock(open(os.path.join(dataroot, "controllers.json"), "r+")) as f:
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
                ssh, _ = util.build_ssh_commands(c)
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
            installer.do_server_install(args.package)
        except installer.InstallException as e:
            logging.error("Failed to install server: {}".format(str(e)))
            return 1

        try:
            installer.do_client_installs(args.package)
        except installer.InstallException as e:
            logging.error("Failed to install clients: {}".format(str(e)))
            return 1

        # TODO:
        # [ ] Once done, return success/failure and pack up logs as a compressed tarball
    else:
        try:
            installer.do_appcontroller_install(args.package)
        except installer.InstallException as e:
            logging.error("Failed to install requirements for package {}".format(args.package))
            return 1

    return 0

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Manage NetIDE packages")
    subparsers = parser.add_subparsers()

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
