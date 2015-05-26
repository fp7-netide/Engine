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

import os
import subprocess
import sys
import time
import uuid

import requests

class Base(object):
    applications = []

    def __init__(self, dataroot):
        self.dataroot = dataroot
        self.id = str(uuid.uuid4())
        self.logdir = os.path.join(self.dataroot, "logs", self.id)
        os.makedirs(self.logdir, exist_ok=True)

    @classmethod
    def version(cls):
        "Returns either the version of the controller as a string or None if the controller is not installed"
        return None

    def start(self):
        "Starts the controller and all its applications. Returns a list of dictionaries of the following form:"
        "{ \"id\": process-uuid, \"pid\": process-id }"
        raise NotImplementedError()

class Ryu(Base):
    @classmethod
    def version(cls):
        try:
            v = subprocess.check_output(["ryu", "--version"], stderr=subprocess.STDOUT).decode("utf-8")
            return v.strip().split(" ", 1)[1]
        except subprocess.CalledProcessError:
            return None
        except FileNotFoundError:
            return None

    def start(self):
        cmdline = ["sudo", "ryu-manager", "--ofp-tcp-listen-port=6633"]
        args = []
        ppath = []
        for a in self.applications:
            if not a.enabled:
                print("Skipping disabled application {}".format(a), file=sys.stderr)
                continue
            ppath.append(os.path.abspath(os.path.relpath(a.path)))
            p = a.metadata.get("param", "")
            def f(x):
                pt = os.path.join(a.path, x)
                if os.path.exists(pt):
                    return pt
                return x
            if isinstance(p, list):
                args.extend(map(f, p))
            else:
                args.append(f(p))
        cmdline.extend(sorted(args))
        print('Launching "{}" now'.format(cmdline), file=sys.stderr)
        env = os.environ.copy()
        ppath.extend(env.get("PYTHONPATH", "").split(":"))
        env["PYTHONPATH"] = ":".join(ppath)
        serr = open(os.path.join(self.logdir, "stderr"), "w")
        sout = open(os.path.join(self.logdir, "stdout"), "w")
        return [{ "id": self.id, "pid": subprocess.Popen(cmdline, stderr=serr, stdout=sout, env=env).pid }]

class FloodLight(Base):
    def __init__(self, entrypoint=""):
        self.entrypoint = entrypoint

    def __str__(self):
        return "FloodLight({})".format(self.entrypoint)

    @classmethod
    def version(cls):
        v = subprocess.check_output(["cd ~/floodlight; git describe; exit 0"], shell=True, stderr=subprocess.STDOUT)
        return v.decode("utf-8").split("-")[0]

    def running(self):
        "Returns True if there's an instance of floodlight running on the local host"
        try:
            # TODO: fancier detection, this is quite broad
            r = requests.get("http://127.0.0.1:8080/")
            # if r.status_code != 200:
            #    return False
            return True
        except requests.exceptions.ConnectionError:
            return False

    def start(self):
        return [{ "id": str(uuid.uuid4()), "pid": -1 }]
        # print("Starting {!s}".format(self), file=sys.stderr)
#        if not self.running():
#            cpid = subprocess.Popen(["cd ~/floodlight; ./floodlight.sh"], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL, shell=True).pid
#            print("Waiting for floodlight to come up...", file=sys.stderr)
#            while not self.running():
#                time.sleep(2)
#                print("waiting...", file=sys.stderr)
#            print("done waiting", file=sys.stderr)
#        return subprocess.Popen([self.entrypoint], shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).pid

class ODL(Base):
    # TODO:
    # - determine canonical path to karaf
    #   - require path specification
    # - check version/state of karaf/bundles/features
    # - install missing bundles/features?
    #   - should be done automatically when installing/starting application bundle
    # - start controller if not already done
    # - install/start app bundle
    pass

class POX(Base):
    pass

class Pyretic(Base):
    pass
