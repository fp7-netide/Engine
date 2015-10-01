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
import logging
import os
import platform
import shutil
import tempfile
import zipfile

from loader import controllers
from loader.application import Application

class Package(object):
    config = {}
    controllers = {}
    cleanup = False

    def __init__(self, prefix, dataroot):
        self.path = os.path.abspath(prefix)
        if prefix.endswith(".zip") and os.path.isfile(self.path):
            p = tempfile.mkdtemp(prefix="netide-tmp")
            with zipfile.ZipFile(self.path) as zf:
                zf.extractall(path=p)
            self.path = p
            self.cleanup = True

        p = os.path.join(self.path, "controllers.json")
        if os.path.exists(p):
            with open(p) as f:
                self.config = json.load(f)

        logging.debug("Loading applications for host {}".format(platform.node()))
        p = os.path.join(self.path, "_apps")
        for d in os.listdir(p):
            if d not in self.config.get("clients", {}).get(platform.node(), {}).get("apps", []):
                logging.debug("Skipping application {} (not for this host)".format(d))
                continue
            logging.debug("Loading app metadata for {} on {}".format(d, platform.node()))
            app = os.path.join(p, d)
            ctrl = Application.get_controller(app)
            if ctrl not in self.controllers:
                self.controllers[ctrl] = ctrl(dataroot)
            self.controllers[ctrl].applications.append(Application(app))

    def __del__(self):
        if self.cleanup:
            shutil.rmtree(self.path, ignore_errors=True)

    def __str__(self):
        return 'Package("{}")'.format(self.path)

    def applies(self):
        # FIXME: there's a lot of validation missing here: checking topology and what not
        for c in self.controllers:
            for a in c.applications:
                if not a.valid_requirements():
                    logging.error("Requirements for application {} not met".format(a))
                    return False

        return True

    def start(self):
        return {cls.__name__: { "procs": c.start(), "apps": [str(a) for a in c.applications] }
                for cls, c in self.controllers.items()}

    def get_clients(self):
        clients = [ ]
        for (name, d) in self.config.get("clients", {}).items():
            host = d.get("host", name)
            if "user" in d:
                host = "{}@{}".format(d["user"], host)

            entry = [ host ]
            if "port" in d:
                entry.append(d["port"])
            if "identity" in d:
                entry.append(os.path.expanduser(d["identity"]))
            clients.append(entry)
        return clients
