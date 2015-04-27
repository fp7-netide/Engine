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

class Application(object):
    path = ""

    def __init__(self, prefix):
        self.path = prefix
        print("Reading application {}".format(prefix))

        self.files = []
        for dirname, dirs, files in os.walk(self.path):
            for f in files:
                self.files.append(os.path.join(os.path.abspath(dirname), f))

        p = os.path.join(self.path, "_meta.json")
        if os.path.exists(p):
            with open(p) as f:
                self.metadata = json.load(f)

        # TODO: make sure the controller required is installed, and that requirements cross-match
        #       with what's in _system_requirements.json

    def __repr__(self):
        return 'Application("{}", Metadata: {})'.format(self.path, self.metadata)

class Package(object):
    requirements = {}
    parameters   = {}
    applications = []
    path = ""

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

        assert self.valid()

    def __str__(self):
        return 'Package("{}", Requirements: {}, Parameters: {}, Applications: {})'.format(self.path,
                self.requirements, self.parameters, self.applications)

    def valid_hardware(self):
        # TODO
        return True

    def valid_software(self):
        # TODO
        return True

    def valid_network(self):
        # TODO
        return True

    def valid(self):
        return self.valid_hardware() and self.valid_software() and self.valid_network()

if __name__ == "__main__":
    p = Package(sys.argv[1])
    print(p)
