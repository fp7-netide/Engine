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

import inspect
import json
import os
import logging
import configparser
import ast
import re
import io

from yaml import load
from loader import controllers
from loader import environment
from functools import reduce

class Application(object):
    metadata = {}

    def __init__(self, prefix):
        self.path = prefix
        self.files = []
        for dirname, dirs, files in os.walk(self.path):
            for f in files:
                self.files.append(os.path.join(os.path.abspath(dirname), f))

    #doesn't exist anymore !!
        p = os.path.join(self.path, "_meta.json")
        if os.path.exists(p):
            with open(p) as f:
                self.metadata = json.load(f)

        self.enabled = self.metadata.get("enabled", True)

    def __str__(self):
        return os.path.basename(self.path)


    def valid_requirements(self):
        reqs = self.metadata.get("requirements", {})

        # Return True if all requirements are met
        # Check Controllers
        try:
            environment.check_controllers(reqs.get("Software", {}).get("Controllers", {}))
        except environment.ControllerCheckException as e:
            logging.error("Controller check failed: {}".format(str(e)))
            return False

        # TODO: Check libraries
        try:
            environment.check_languages(reqs.get("Software", {}).get("Languages", {}))
        except environment.LanguageCheckException as e:
            logging.error("Missing depency: {}".format(str(e)))
            return False

        try:
            environment.check_hardware(reqs.get("Hardware", {}))
        except environment.HardwareCheckException as e:
            logging.error("Hardware configuration mismatch: {}".format(str(e)))
            return False
        # TODO: Check network
        return True


    @classmethod
    def get_controller(cls, path):
        test = os.path.basename(path) + ".sysreq"
       
    
        with open(os.path.join(path, test)) as f:
            
            s = io.StringIO()

            st = reduce(lambda x, y: x + y, f)
            f.seek(0)

            enclosed = st.lstrip()[0] == "{"

            if not enclosed: s.write("{")
            for line in f: s.write(line.replace("\t", "  ").replace(":\"", ": "))
            if not enclosed: s.write("}")
            
            s.seek(0)
            
            y = load(s)
            
            c = y.get("app").get("controller").get("name")

            return {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c.lower())
