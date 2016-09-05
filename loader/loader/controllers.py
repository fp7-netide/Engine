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

import json
import logging
import os
import subprocess
import sys
import time
import uuid
import yaml
from functools import reduce
from subprocess import call

from loader import util

class Base(object):
    applications = []

    def __init__(self, dataroot):
        self.dataroot = dataroot

    def makelogdir(self, myid):
        d = os.path.join(self.dataroot, "logs", myid)
        os.makedirs(d, exist_ok=True)
        return d

    @classmethod
    def attachTmux(cls):
        sessionExists = call(["tmux", "has-session", "-t", "NetIDE"])

        if [ sessionExists != 0 ]:
            call("tmux attach -t NetIDE", shell=True)
        else:
            print("Tmux Session NetIDE was not found!")

    @classmethod
    def getControllerName(cls):
        "Returns the name of the controller"
        return None
    @classmethod
    def version(cls):
        "Returns either the version of the controller as a string or None if the controller is not installed"
        return None

    def start(self):
        "Starts the controller and all its applications. Returns a list of dictionaries of the following form:"
        "{ \"id\": process-uuid, \"pid\": process-id }"
        raise NotImplementedError()

class RyuShim(Base):

    def __init__(self, ofport="6633"):
        self.port = ofport

    def start(self):
        base = ["ryu-manager"]

        y = yaml.load(open("commands.yml"))
        ryuCommands = y['ryu_shim']

        v = os.environ.copy()
        v.update(ryuCommands["variables"])

        sessionExists = call(["tmux", "has-session", "-t", "NetIDE"])

        if [ sessionExists != 0 ]:
            call(["tmux", "new-session", "-d", "-s", "NetIDE"], env=v)
            time.sleep(1)

        cmd = util.tmux_line(ryuCommands)

        list = util.getWindowList()

        if "RyuShim" not in list:
            #6633 default for port listening
            newCmd = "bash -c \'cd ~/netide/Engine/ryu-shim/ && " + cmd + " --ofp-tcp-listen-port=" + self.port + " ryu-shim.py\'"
            call(['tmux', 'new-window', '-n', "RyuShim", '-t', 'NetIDE', newCmd])

        else:
            print("Ryu Shim already running")



class Ryu(Base):
    appNames = []
    ryubackendport = 7733

    @classmethod
    def getControllerName(cls):
        return "ryu"

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
        base = ["ryu-manager"]

        y = yaml.load(open("commands.yml"))
        ryuCommands = y['ryu']

        v = os.environ.copy()
        v.update(ryuCommands["variables"])

        sessionExists = call(["tmux", "has-session", "-t", "NetIDE"])

        if [ sessionExists != 0 ]:
            call(["tmux", "new-session", "-d", "-s", "NetIDE"], env=v)
            time.sleep(1)

        cmd = util.tmux_line(ryuCommands)



        for a in self.applications:


            self.appNames.append(a.appName)

            appPath = os.path.join(a.path, a.entrypoint)

            #check with list window if exists
            windowNames = util.getWindowList()

            if a.appName not in windowNames:

                ryubackendpath = "bash -c \' cd ~/netide/Engine/ryu-backend/ && " + cmd + " --ofp-tcp-listen-port=" + str(Ryu.ryubackendport) + " ryu-backend.py " + os.path.join(a.path, a.entrypoint) + "\' "

                call(['tmux', 'new-window', '-n', a.appName, '-t', 'NetIDE', ryubackendpath])
            
                Ryu.ryubackendport = Ryu.ryubackendport + 1
                #call(['tmux', 'send-keys', '-t', 'NetIDE' ,cmd, ' --ofp-tcp-listen-port=' +str(a.appPort) + " " + os.path.join(a.path, a.entrypoint), 'C-m'])
            else:
                print("app " + a.appName + " already running")

            if "sleepafter" in ryuCommands:
                time.sleep(ryuCommands["sleepafter"])



class FloodLight(Base):
    @classmethod
    def getControllerName(cls):
        return "floodlight"

    @classmethod
    def version(cls):
        pass

    def start(self):
        pass

#===============================================================================
#     @classmethod
#     def version(cls):
#         v = subprocess.check_output(["cd ~/floodlight; git describe; exit 0"], shell=True, stderr=subprocess.STDOUT)
#         return v.decode("utf-8").split("-")[0].strip()
#
#     def start(self):
#         # XXX: application modules are not copied into floodlight right now, they need to be copied manually
#         try:
#             with util.FLock(open(os.path.join(self.dataroot, "controllers.json"), "r"), shared=True) as fh:
#                 data = json.load(fh)
#         except Exception as err:
#             logging.debug("{}: {}".format(type(err), err))
#             data = {}
#         running_apps = data.get("controllers", {}).get("Ryu", {}).get("apps", [])
#
#         for a in self.applications:
#             if not a.enabled:
#                 logging.info("Skipping disabled application {}".format(a))
#                 continue
#             if a in running_apps:
#                 logging.info("App {} is already running".format(a))
#                 continue
#
#             prefix = os.path.expanduser("~/floodlight/src/main/resources")
#
#             with open(os.path.join(prefix, "META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule"), "r+") as fh:
#                 fh.write("{}\n".format(a.metadata.get("param", "")))
#
#             with open(os.path.join(prefix, "floodlightdefault.properties"), "r+") as fh:
#                 lines = fh.readlines()
#                 idx = 0
#                 for l in lines:
#                     if not l.strip().endswith('\\'):
#                         break
#                     idx += 1
#                 lines.insert(idx, "{}\n".format(a.metadata.get("param", "")))
#                 fh.seek(0)
#                 fh.truncate()
#                 fh.writelines(lines)
#
#         myid = str(uuid.uuid4())
#         logdir = self.makelogdir(myid)
#
#         serr = open(os.path.join(logdir, "stderr"), "w")
#         sout = open(os.path.join(logdir, "stdout"), "w")
#
#         # Rebuild floodlight with ant
#         cmdline = ["cd ~/floodlight; ant"]
#         subprocess.Popen(cmdline, stderr=serr, stdout=sout, shell=True).wait() # TODO: check exit value?
#
#         # Start floodlight
#         cmdline = ["cd ~/floodlight; java -jar floodlight/target/floodlight.jar"]
#         return [{ "id": myid, "pid": subprocess.Popen(cmdline, stderr=serr, stdout=sout, shell=True).id }]
#===============================================================================

class Core(Base):
    packagePath = ""
    def __init__(self, path):
        self.packagePath = path

    def start(self):

        y = yaml.load(open("commands.yml"))
        coreCommands = y['core']

        sessionExists = call(["tmux", "has-session", "-t", "NetIDE"])

        if [ sessionExists != 0 ]:
            call(["tmux", "new-session", "-d", "-s", "NetIDE"])
            time.sleep(1)
        list = util.getWindowList()

        if "Core" not in list:

            call(['tmux', 'new-window', '-n', "Core", '-t', 'NetIDE', "bash -c \'cd ~/netide/core-karaf/bin/ && ./karaf\'"])

            compositionPath = os.path.join(self.packagePath, "composition/composition.xml")


            time.sleep(coreCommands['sleepafter'])

            call(['tmux', 'send-keys', '-t', 'NetIDE' , "netide:loadcomposition "+compositionPath, 'C-m'])
            #call(['tmux', 'send-keys', '-t', 'NetIDE' , "log:tail", 'C-m'])

        else:
            print("Core already running")



class ODL(Base):

    def start(self):

        y = yaml.load(open("commands.yml"))
        odlCommands = y['odl']

        sessionExists = call(["tmux", "has-session", "-t", "NetIDE"])

        if [ sessionExists != 0 ]:
            call(["tmux", "new-session", "-d", "-s", "NetIDE"])

        list = util.getWindowList()

        if "ODL" not in list:

            call(['tmux', 'new-window', '-n', "ODL", '-t', 'NetIDE', '~/netide/distribution-karaf-0.4.0-Beryllium/bin/karaf'])

            time.sleep(odlCommands['sleepafter'])
        else:
            print("ODL already running")

class Mininet(Base):
    def start(self):

        y = yaml.load(open("commands.yml"))
        mininetCommands = y['mininet']
        sessionExists = call(["tmux", "has-session", "-t", "NetIDE"])

        if [ sessionExists != 0 ]:
            call(["tmux", "new-session", "-d", "-s", "NetIDE"])
            time.sleep(1)

        list = util.getWindowList()

        if Mininet not in list:
            call(['tmux', 'new-window', '-n', "Mininet", '-t', 'NetIDE', "sudo python ~/Engine/loader/Demo/gen/mininet/Demo_run.py"])
            #call(['tmux', 'send-keys', '-t', 'NetIDE' , "sudo python ~/Engine/loader/Demo/gen/mininet/Demo_run.py", 'C-m'])
            time.sleep(mininetCommands['sleepafter'])
        else:
            print("Mininet already running")


class POX(Base):
    pass

class Pyretic(Base):
    pass
