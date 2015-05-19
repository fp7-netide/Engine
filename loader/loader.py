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
# - Read metadata
# - Collect Apps
#   - Check parameter constraints (should always apply, but better be safe)
# - Determine App->Controller mappings
# - For each app:
#   - Check system requirements
#     - Hardware: CPU/RAM
#     - Installed Software: Java version? Controller software? ...?
#     - If controller is not yet running:
#       - Start controller
#     - Start application

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
import os
import signal
import sys
import time

import controllers

datapath = "/tmp/netide-controllers.json"

class Application(object):
    metadata = {}

    def __init__(self, prefix):
        self.path = prefix

        self.files = []
        for dirname, dirs, files in os.walk(self.path):
            for f in files:
                self.files.append(os.path.join(os.path.abspath(dirname), f))

        p = os.path.join(self.path, "_meta.json")
        if os.path.exists(p):
            with open(p) as f:
                self.metadata = json.load(f)

        self.enabled = self.metadata.get("enabled", True)

    def __str__(self):
        return 'App({})'.format(self.path)

    @classmethod
    def get_controller(cls, path):
        with open(os.path.join(path, "_meta.json")) as f:
            m = json.load(f)
            c = m.get("controller")
            if c is None:
                return None
            return {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c.lower())


class Package(object):
    requirements = {}
    controllers = {}

    def __init__(self, prefix):
        self.path = os.path.abspath(prefix)
        p = os.path.join(prefix, "_system_requirements.json")
        if os.path.exists(p):
            with open(p) as f:
                self.requirements = json.load(f)

        p = os.path.join(prefix, "_apps")
        for d in os.listdir(p):
            app = os.path.join(p, d)
            ctrl = Application.get_controller(app)
            if ctrl not in self.controllers:
                self.controllers[ctrl] = ctrl()
            self.controllers[ctrl].applications.append(Application(app))

    def __str__(self):
        return 'Package("{}")'.format(self.path)

    def valid_requirements(self):
        # Return True if all requirements are met
        # Check Software
        # TODO: Allow wildcards in versions? re matching?
        for c in self.requirements.get("Software", {}).get("Controllers", {}):
            cls = {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c["name"].lower())
            if cls is None:
                print("Not checking for unknown controller {}".format(c))
                continue
            v = cls.version()
            if v != c["version"]:
                print("Expected {} version {}, got {}".format(cls.__name__, c["version"], v), file=sys.stderr)
                return False
        # TODO: Check libraries
        # TODO: Check languages
        # TODO: Check hardware
        # TODO: Check network
        return True

    def applies(self):
        # FIXME: there's a lot of validation missing here: checking topology and what not
        if not self.valid_requirements():
            print("At least one requirement was not met")
            return False

        # Make sure all controllers listed in applications actually appear in our requirements file
        ctrls = { x.get("name") for x in self.requirements.get("Software", {}).get("Controllers", {}) }
        for c in self.controllers.keys():
            cname = c.__name__.lower()
            if cname not in ctrls:
                print("Could not find controller {} in the list of required controllers".format(cname), file=sys.stderr)
                return False

        # TODO: warn about unused controllers?
        return True

    def start(self):
        rv = {}
        for (cls, c) in self.controllers.items():
            rv[cls.__name__] = { "pids": c.start(), "apps": [str(a) for a in c.applications] }
        return rv

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
    p = Package(args.package)
    if not p.applies():
        print("There's something wrong with the package", file=sys.stderr)
        return 2

    # TODO: store {pids,logs} somewhere in /var/{run,log}
    with FLock(open(datapath, "w+")) as f:
        try:
            data  = json.load(f)
        except ValueError:
            data = {}
        f.seek(0)
        f.truncate()
        try:
            pids = p.start()
            print(pids)
            data["controllers"] = pids
            json.dump(data, f, indent=2)
        except:
            return 1
    return 0

def list_controllers(args):
    try:
        with FLock(open(datapath), fcntl.LOCK_SH) as f:
            print(f.read())
        return 0
    except Exception as err:
        print(err, file=sys.stderr)
        return 1

def stop_controllers(args):
    with FLock(open(datapath, "r+")) as f:
        try:
            d = json.load(f)
            for c in d["controllers"]:
                for pid in d["controllers"][c]["pids"]:
                    try:
                        # TODO: gentler (controller specific) way of shutting down?
                        os.kill(pid, signal.SIGTERM)
                        print("Sent a SIGTERM to process {} for controller {}".format(pid, c), file=sys.stderr)
                        time.sleep(5)
                        os.kill(pid, signal.SIGKILL)
                        print("Sent a SIGKILL to process {} for controller {}".format(pid, c), file.sys.stderr)
                    except ProcessLookupError:
                        pass
            f.seek(0)
            f.truncate()
            del d["controllers"]
            json.dump(d, f)
        except Exception as err:
            print(err, file=sys.stderr)
            return 1
    return 0

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Manage NetIDE packages")
    subparsers = parser.add_subparsers()

    parser_load = subparsers.add_parser("load", description="Load a NetIDE package and start its applications")
    parser_load.add_argument("package", type=str, help="Package to load")
    parser_load.set_defaults(func=load_package)

    parser_list = subparsers.add_parser("list", description="List currently running NetIDE controllers")
    parser_list.set_defaults(func=list_controllers)

    parser_stop = subparsers.add_parser("stop", description="Stop all currently runnning NetIDE controllers")
    parser_stop.set_defaults(func=stop_controllers)

    args = parser.parse_args()
    if 'func' not in vars(args):
        parser.print_help()
        sys.exit(1)
    sys.exit(args.func(args))
