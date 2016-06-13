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
import hashlib
import tarfile

from loader import controllers
from loader import util
from loader.application import Application

class Package(object):
    config = {}
    controllers = {}
    cleanup = False
    controllerNames = []

    def __init__(self, prefix, dataroot):
        self.dataroot = dataroot
        self.path = os.path.abspath(prefix)
        
        if os.path.isfile(self.path):
            if (tarfile.is_tarfile(self.path)):
           
               self.path = util.extractPackage(self.path)
        

        p = os.path.join(self.path, "controllers.json")

        if not os.path.exists(p):
            with open(p, "w") as f:
                json.dump({'clients': {'vagrant-ubuntu-trusty-64' : {'identity' : '~/testvm-bare/.vagrant/machines/default/virtualbox/private_key', 'port' : 2222, 'host' : 'localhost', 'user' : 'vagrant', 'apps' : [] }}, 'server': {'type' : 'odl',  'host' : 'localhost'}}, f, indent= 4)
        
        with open(p) as f:
            self.config = json.load(f)
           #     self.appNames = self.config.keys()
            #    for key in self.appNames:
             #        print(key)


                
        logging.debug("Loading applications for host {}".format(platform.node()))
        p = os.path.join(self.path, "apps")
        self.appNames = []
        for d in os.listdir(p):
            if not d == ".DS_Store":
                nodes = []
                #print(self.config.get("clients", {}).items())
                self.appNames.append(d)
                #nodes.append(d)  
                for node, v in self.config.get("clients", {}).items():
                    nodes.append(node)
                    
                logging.debug("Loading app metadata for {} on {}".format(d, platform.node()))
                app = os.path.join(p, d)
                
                #returns controller
                ctrl = Application.get_controller(app)
                self.controllerNames.append(Application.get_controller_name(app))

                for n in nodes:
                    if n not in self.controllers:
                        self.controllers[n] = {}
                        
                    if ctrl not in self.controllers[n]:
                        self.controllers[n][ctrl] = ctrl(dataroot)

                    self.controllers[n][ctrl].applications.append(Application(app))



        hash = hashlib.sha1()
        for (dirpath, dirnames, filenames) in os.walk(self.path):
            for f in filenames:
                hash.update(bytes(f, 'utf-8'))
                with open(os.path.join(dirpath, f), 'rb') as fh:
                    hash.update(fh.read())
        self.cksum = hash.hexdigest()

    def __del__(self):
        if self.cleanup:
            shutil.rmtree(self.path, ignore_errors=True)

    def __str__(self):
        return 'Package("{}")'.format(self.path)

    def controllers_for_node(self, node=None):

        if node is None:
            node = platform.node()
        return self.controllers[node]

    def applies(self, dataroot=None):
        # FIXME: there's a lot of validation missing here: checking topology and what not
        if dataroot is None:
            dataroot = self.dataroot

        for c in self.controllers_for_node():
            for a in c.applications:
                if not a.valid_requirements():
                    logging.error("Requirements for application {} not met".format(a))
                    return False

        p = os.path.join(dataroot, "controllers.json")
        if not os.path.exists(p):
            logging.debug("No {} to check".format(p))
            return True

        with util.FLock(open(p, "r"), shared=True) as fh:
            try:
                data = json.load(fh)
                if self.cksum != data["cksum"]:
                    logging.debug("{} != {}".format(self.cksum, data["cksum"]))
                    # XXX
                    # return False
            except Exception as e:
                logging.error("{}: {} ({})".format(str(type(e)), e, dataroot))
                fh.seek(0)
                logging.debug("{}".format(fh.read()))
                return False

        return True

    def start(self, data):
        for cls, c in self.controllers_for_node().items():
            n = cls.__name__

            p = data.get(n, {}).get("procs", [])
            p.extend(c.start())

            a = set(data.get(n, {}).get("apps", [])) | {str(app) for app in c.applications}

            data[n] = { "procs": p, "apps": list(a) }

        return data

    def get_composition(self):
        try:
            with open(os.path.join(self.path, "composition.comp"), "r") as fh:
                return fh.read()
        except FileNotFoundError:
            return ""

    def get_clients(self):
        clients = [ ]

        p = os.path.join(self.path, "controllers.json")
        if os.path.exists(p):

            for (name, d) in self.config.get("clients", {}).items():
                host = d.get("host", name)
    
                if "user" in d:
                    host = "{}@{}".format(d["user"], host)
        
                entry = [ name, host ]
                if "port" in d:
                    entry.append(d["port"])
                if "identity" in d:
                    entry.append(os.path.expanduser(d["identity"]))
                clients.append(entry)

        return clients
