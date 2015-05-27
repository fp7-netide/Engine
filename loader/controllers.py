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
        cmdline = ["sudo", "ryu-manager", "--ofp-tcp-listen-port=6633", os.path.expanduser("~/Engine/ryu-shim/ryu_shim.py")]
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
    @classmethod
    def version(cls):
        v = subprocess.check_output(["cd ~/floodlight; git describe; exit 0"], shell=True, stderr=subprocess.STDOUT)
        return v.decode("utf-8").split("-")[0].strip()

    def start(self):
        # XXX: application modules are not copied into floodlight right now, they need to be copied manually
        for a in self.applications:
            if not a.enabled:
                print("Skipping disabled application {}".format(a), file=sys.stderr)
                continue

            prefix = os.path.expanduser("~/floodlight/src/main/resources")

            with open(os.path.join(prefix, "META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule"), "r+") as fh:
                fh.write("{}\n".format(a.metadata.get("param", "")))

            with open(os.path.join(prefix, "floodlightdefault.properties"), "r+") as fh:
                lines = fh.readlines()
                idx = 0
                for l in lines:
                    if not l.strip().endswith('\\'):
                        break
                    idx += 1
                lines.insert(idx, "{}\n".format(a.metadata.get("param", "")))
                fh.seek(0)
                fh.truncate()
                fh.writelines(lines)

        serr = open(os.path.join(self.logdir, "stderr"), "w")
        sout = open(os.path.join(self.logdir, "stdout"), "w")

        # Rebuild floodlight with ant
        cmdline = ["cd ~/floodlight; ant"]
        subprocess.Popen(cmdline, stderr=serr, stdout=sout, shell=True).wait() # TODO: check exit value?

        # Start floodlight
        cmdline = ["cd ~/floodlight; java -jar floodlight/target/floodlight.jar"]
        return [{ "id": self.id, "pid": subprocess.Popen(cmdline, stderr=serr, stdout=sout, shell=True).id }]

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
