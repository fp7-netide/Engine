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

import json
import sys
import os
import inspect

import controllers

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

    def __repr__(self):
        return 'Application("{}")'.format(self.path)

    @classmethod
    def get_controller(cls, path):
        with open(os.path.join(path, "_meta.json")) as f:
            m = json.load(f)
            c = m.get("controller")
            if c is None:
                return None
            return dict(map(lambda i: (i[0].lower(), i[1]), inspect.getmembers(controllers))).get(c.lower())


class Package(object):
    requirements = {}

    def __init__(self, prefix):
        self.path = os.path.abspath(prefix)
        p = os.path.join(prefix, "_system_requirements.json")
        if os.path.exists(p):
            with open(p) as f:
                self.requirements = json.load(f)

        self.controllers = {}

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
        # TODO: Check controllers other than ryu
        # TODO: Allow wildcards in versions?
        for c in self.requirements.get("Software").get("Controllers"):
            if c["name"] == "ryu":
                v = controllers.Ryu.version()
                if v != c["version"]:
                    print("Expected Ryu version {}, got {}".format(c["version"], v), file=sys.stderr)
                    return False
            elif c["name"] == "floodlight":
                v = controllers.FloodLight.version()
                if v != c["version"]:
                    print("Expected floodlight version {}, got {}".format(c["version"], v), file=sys.stderr)
                    return False
            else:
                print("Not checking for unknown controller {}".format(c))
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
        ctrls = set(map(lambda x: x.get("name"),
                    self.requirements.get("Software", {}).get("Controllers", {})))
        for c in self.controllers.keys():
            cname = c.__name__.lower()
            if cname not in ctrls:
                print("Could not find controller {} in the list of required controllers".format(cname), file=sys.stderr)
                return False

        # TODO: warn about unused controllers?
        return True

    def start(self):
        pids = []
        for (_, c) in self.controllers.items():
            pids.append(c.start())
        return pids

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Expected a directory", file=sys.stderr)
        sys.exit(1)

    p = Package(sys.argv[1])
    if not p.applies():
        print("There's something wrong with the package", file=sys.stderr)
        sys.exit(2)

    print(p.start())
