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
     Jan-Niclas Struewer, struewer@mail.upb.de
"""


import argparse
import fcntl
import inspect
import json
import logging
import os
import platform
import signal
import sys
import time
import zmq

import subprocess as sp

from loader import controllers
from loader import environment
from loader import installer
from loader import topology
from loader import util
from loader.package import Package
from loader.controllers import ODL
from subprocess import call
from loader.controllers import Base
from loader.controllers import Mininet
from loader.controllers import RyuShim
from loader.controllers import Core
import time

dataroot = "/tmp/netide"
os.environ["ANSIBLE_HOST_KEY_CHECKING"] = "False"
extractPath = os.path.join(dataroot, "extractPath.txt")

logging.basicConfig(format="%(asctime)-15s %(levelname)-7s %(message)s", level=logging.DEBUG)

def extract_package(args):

    util.extractPackage(args.path)

    return 0

def set_extraction_path(args):
    util.setExtractionPath(args.path)

    return 0;

def createParam(args):
    p = Package(args.package, dataroot)
    p.createParamFile(args.file)

def start_package(args):

    if not args.param == None:
        p = Package(args.package, dataroot, args.param)

    else:
        p = Package(args.package, dataroot)

    file_format = "json"
    if not args.format == None:
        file_format = args.format

    if not p.load_apps_and_controller(file_format):
        logging.error("There's something wrong with the package")
        return 2

    Core(p.path).start()


    if args.server == "ryu":
        if args.ofport == None:
            RyuShim().start()
        else:
            RyuShim(args.ofport).start()
    else:

        ODL("").start()
    time.sleep(1)
###### workaround for demo - dirty #######
    if args.mininet == "on":
        mnpath = "bash -c \' sudo mn -c && cd " + p.path + "/Topology && sudo chmod +x UC2_IITSystem.py && sudo ./UC2_IITSystem.py $1 && cd ../ \' "

        call(['tmux', 'new-window', '-n', "mn", '-t', 'NetIDE', mnpath])
        time.sleep(1)
########


    for c in p.controllers_for_node().items():
        c[1].start()

    time.sleep(2)


    attach("")


def attach(args):

    Base.attachTmux()


def list_controllers(args):
    for s in util.getWindowList():
        print(s)

def stop_controllers(args):
    sessionExists = call(["tmux", "has-session", "-t", "NetIDE"])

    if [ sessionExists != 0 ]:
        call(["tmux", "kill-session", "-t", "NetIDE"], shell=False)



    return 0

def get_topology(args):
    if args.host is None:
        print(topology.get("127.0.0.1:8080"))
    else:
        print(topology.get(args.host))
    return 0

def install(args):

    logging.debug(args)
    if args.mode not in ["all", "appcontroller"]:
        logging.error("Unknown installation mode '{}'. Expected one of ['all', 'appcontroller']".format(args.mode))
        return 1
    if args.mode == "all":

        try:
            installer.do_server_install(args.package)
        except installer.InstallException as e:
            logging.error("Failed to install server: {}".format(str(e)))
            return 1

        try:
            installer.do_client_installs(args.package, dataroot)
        except installer.InstallException as e:
            logging.error("Failed to install clients: {}".format(str(e)))
            return 1

    return 0

def generate(args):

    if not args.param == None:
        p = Package(args.package, dataroot, args.param)
    else:
        p = Package(args.package, dataroot)
    if not p.load_apps_and_controller(args.format):
        logging.error("There's something wrong with the package")
        return 2

def check(args):

     with util.TempDir("netide-client-installs") as t:
        p = Package(args.package, t)

     if not p.check_sysreq():
        logging.error("There's something wrong with the package")
        return 2
     else:
        logging.debug("System requirements met for package {}".format(args.package))

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Manage NetIDE packages")
    subparsers = parser.add_subparsers()

    parser_attach = subparsers.add_parser("attach", description="If possible attaches tmux session")
    parser_attach.set_defaults(func=attach, mode="all")

    parser_createParam = subparsers.add_parser("createconf", description="Generates an empty parameters.json file.")
    parser_createParam.add_argument("package", type=str, help="Package for which the param file will be generated")
    parser_createParam.add_argument("--file", type=str, help="Path where the param file will be saved.")
    parser_createParam.set_defaults(func=createParam, mode="all")

    parser_start = subparsers.add_parser("run", description="Load a NetIDE package and start its applications")
    parser_start.add_argument("package", type=str, help="Package to load")
    parser_start.add_argument("--format", type=str, help="Enter py or json to define the created parameter file format")
    parser_start.add_argument("--server", type=str, help="Choose one of {ODL, ryu}")
    parser_start.add_argument("--ofport", type=str, help="Choose port for of.")
    parser_start.add_argument("--param", type=str, help="Path to Param File which should be used to configure the package.")
    parser_start.add_argument("--mininet", type=str, help="enter on to use mininet")
    parser_start.set_defaults(func=start_package, mode="all")

    parser_createHandlebars = subparsers.add_parser("genconfig", description="Generates application configurations from a parameter file.")
    parser_createHandlebars.add_argument("package", type=str, help="Package to use")
    parser_createHandlebars.add_argument("format", type=str, help="Enter py or json to define the created parameter file format")
    parser_createHandlebars.add_argument("--param", type=str, help="Path to Param File which should be used to configure the package.")
    parser_createHandlebars.set_defaults(func=generate, mode="all")

    parser_extract = subparsers.add_parser("extractarchive", description ="extractsArchive")
    parser_extract.add_argument("path", type=str, help="Path to archive")
    parser_extract.set_defaults(func=extract_package, mode="all")

    parser_extract_path = subparsers.add_parser("extractionPath", description ="Set the extraction path to the given argument")
    parser_extract_path.add_argument("path", type=str, help="Path to store extracted package")
    parser_extract_path.set_defaults(func=set_extraction_path, mode="all")



    parser_list = subparsers.add_parser("list", description="List currently running NetIDE controllers")
    parser_list.set_defaults(func=list_controllers)

    parser_stop = subparsers.add_parser("stop", description="Stop all currently runnning NetIDE controllers")

    parser_stop.set_defaults(func=stop_controllers)

   # parser_topology = subparsers.add_parser("gettopology", description="Show network topology")
   # parser_topology.add_argument("host",
   #         type=str, help="Server controller host:port to query, defaults to 127.0.0.1:8080", nargs="?")
   # parser_topology.set_defaults(func=get_topology)

    parser_install = subparsers.add_parser("install",
            description="Prepare machines listed in `package' by installing required software")
    parser_install.add_argument("--mode",
            type=str, help="Installation mode, one of {appcontroller,all}, defaults to all")
    parser_install.add_argument("package", type=str, help="Package to prepare for")
    parser_install.set_defaults(func=install, mode="all")

    parser_check = subparsers.add_parser("check",
            description="Check system requirements for each application in `package'")
    parser_check.add_argument("package", type=str, help="Package to check")
    parser_check.set_defaults(func=check)

    args = parser.parse_args()
    if 'func' not in vars(args):
        parser.print_help()
        sys.exit(1)
    if "mode" in vars(args) and args.mode == "appcontroller":
        f = logging.Formatter("Client: %(levelname)-7s %(message)s")
        l = logging.getLogger()
        l.handlers[0].setFormatter(f)
    sys.exit(args.func(args))
