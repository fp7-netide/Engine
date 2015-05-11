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

import subprocess
import sys
import time

import requests

class Base(object):
    def version(self):
        return None

    def start(self):
        raise NotImplementedError()

class Ryu(Base):
    name = "ryu"
    params = "--ofp-tcp-listen-port={}"

    def __init__(self, port=0, entrypoint=""):
        self.port = port
        self.entrypoint = entrypoint

    def __str__(self):
        return 'RyuController(port={}, entrypoint={})'.format(self.port, self.entrypoint)

    def version(self):
        "Returns either the version of the controller as a string or None if the controller is not installed"
        try:
            v = subprocess.check_output(["ryu", "--version"], stderr=subprocess.STDOUT).decode("utf-8")
            return v.strip().split(" ", 1)[1]
        except subprocess.CalledProcessError:
            return None
        except FileNotFoundError:
            return None

    def start(self):
        cmdline = ["sudo", "ryu-manager", self.params.format(self.port)]
        cmdline.append(self.entrypoint)
        print('Launching "{}" now'.format(cmdline))
        return subprocess.Popen(cmdline).pid

class FloodLight(Base):
    name = "floodlight"

    def __init__(self, entrypoint=""):
        self.entrypoint = entrypoint

    def __str__(self):
        return "FloodLight({})".format(self.entrypoint)

    def version(self):
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
        print("Starting {!s}".format(self), file=sys.stderr)
        if not self.running():
            cpid = subprocess.Popen(["cd ~/floodlight; ./floodlight.sh"], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL, shell=True).pid
            print("Waiting for floodlight to come up...", file=sys.stderr)
            while not self.running():
                time.sleep(2)
                print("waiting...", file=sys.stderr)
            print("done waiting", file=sys.stderr)
        return subprocess.Popen([self.entrypoint], shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).pid
