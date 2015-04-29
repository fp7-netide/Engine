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
#     - Apply parameters
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

from controllers import RyuController

class Application(object):
    metadata   = {}
    controller = None

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

        if self.metadata.get("controller") == "ryu":
            self.controller = RyuController(self.metadata.get("port", 0),
                    os.path.join(os.path.abspath(self.path), self.metadata.get("entrypoint", "")))
        else:
            raise Exception('Unknown controller "{}"'.format(self.metadata.get("controller")))

    def __repr__(self):
        return 'Application("{}")'.format(self.path)

    def start(self):
        pid = self.controller.start()
        print("Controller started with pid {}".format(pid))
        return pid

class Package(object):
    requirements = {}
    parameters   = {}
    applications = []

    def __init__(self, prefix):
        self.path = os.path.abspath(prefix)
        p = os.path.join(prefix, "_system_requirements.json")
        if os.path.exists(p):
            with open(p) as f:
                self.requirements = json.load(f)

        p = os.path.join(prefix, "_parameters.json")
        if os.path.exists(p):
            with open(p) as f:
                self.parameters = json.load(f)

        p = os.path.join(prefix, "_apps")
        for d in os.listdir(p):
            self.applications.append(Application(os.path.join(p, d)))

    def __str__(self):
        return 'Package("{}", Applications: "{}")'.format(self.path, self.applications)

    def applies(self):
        # FIXME: there's a lot of validation missing here: checking topology and what not

        # Make sure all controllers listed in applications actually appear in our requirements file
        ctrls = set(map(lambda x: x.get("name"),
                    self.requirements.get("Software", {}).get("Controllers", {})))
        for a in self.applications:
            if a.metadata.get("controller") not in ctrls:
                return False

        return True

    def start(self):
        # TODO: dependencies between applications?
        for a in self.applications:
            print("Starting {}".format(a))
            a.start()

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Expected a directory", file=sys.stderr)
        sys.exit(1)

    p = Package(sys.argv[1])
    if not p.applies():
        print("There's something wrong with the package", file=sys.stderr)
        sys.exit(2)

    p.start()
