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

import sys
import platform
from psutil import virtual_memory
from loader import util
import os
import multiprocessing


class Application(object):
    metadata = {}

    def __init__(self, prefix):
        self.path = prefix
        self.files = []
        self.entrypoint = ""

        for dirname, dirs, files in os.walk(self.path):
            for f in files:
                self.files.append(os.path.join(os.path.abspath(dirname), f))

        self.loadSysreq(self.path)

    def __str__(self):
        return os.path.basename(self.path)

    def loadSysreq(self, path):
        sysreqPath = os.path.basename(path) + ".sysreq"
       
    
        with open(os.path.join(path, sysreqPath)) as f:
            
            s = io.StringIO()

            st = reduce(lambda x, y: x + y, f)
            f.seek(0)

            enclosed = st.lstrip()[0] == "{"

            if not enclosed: s.write("{")
            for line in f: s.write(line.replace("\t", "  ").replace(":\"", ": "))
            if not enclosed: s.write("}")
            
            s.seek(0)
            
            y = load(s)
            
            self.entrypoint = y.get("app").get("controller").get("entrypoint")
            self.appName = y.get("app").get("name")
            self.appPort = y.get("app").get("controller").get("port")   

    @classmethod
    def valid_requirements(cls,path):

        requirements_met = True

        #logging.debug("Entered valid_requirements")
        #System requirements
        cpu_req = Application.get_cpu(path)
        ram_req = Application.get_RAM_size(path)
        os_req = Application.get_OS(path)
        sw_req = Application.get_softReq(path)
 
        #Actual system

        #Hardware
        real_cpu = multiprocessing.cpu_count()
        if real_cpu < cpu_req:
         logging.error("CPU requirement not met")
         requirements_met = False
        real_RAM = virtual_memory().total/(1024*1024)
        if real_RAM < ram_req:
         logging.error("RAM requirement not met")
         requirements_met = False
        real_op_sys = sys.platform
        if 'linux' in real_op_sys:
         real_op_sys = 'linux'
        if (os_req!= 'any') and (os_req != real_op_sys):
         logging.error("OS requirement not met")
         requirements_met = False

        #Software
        for sw in sw_req:
            if not util.is_sw_installed(sw_req[sw]["name"]):
                logging.error("Software {} is not installed".format(sw_req[sw]["name"]))
                requirements_met = False
            #Check if sw version fulfills the requirements
            elif not util.check_sw_version(sw_req[sw]["name"], sw_req[sw]["version"]):
                logging.error("Software version requirement for {} not met".format(sw_req[sw]["name"]))
                requirements_met = False


        #If everything's correct
        return requirements_met

    @classmethod
    def parse_sysreq(cls, path):
        sysreqPath = os.path.basename(path) + ".sysreq"
       
    
        with open(os.path.join(path, sysreqPath)) as f:
            
            s = io.StringIO()

            st = reduce(lambda x, y: x + y, f)
            f.seek(0)

            enclosed = st.lstrip()[0] == "{"

            if not enclosed: s.write("{")
            prev_line = ''
            sw_count = 0
            for line in f:
                
                new_line = line.replace("\t", "  ").replace(":\"", ": ")

                if "software:" in prev_line:
                    prev_line = prev_line.replace("software:", "software" + str(sw_count) + ":")
                    sw_count += 1

                #print(prev_line)
                s.write(prev_line)
                prev_line = new_line
            s.write(prev_line)
            if not enclosed: s.write("}")

            s.seek(0)
            #print(s.getvalue())
            y = load(s)

            return y

    @classmethod
    def get_controller_name(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("controller").get("name")

            return c

    @classmethod
    def get_controller(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("controller").get("name")

            return {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c.lower())

    @classmethod
    def get_cpu(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("hardwareReq").get("CPU")

            return int(c)

    @classmethod
    def get_RAM_size(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("hardwareReq").get("RAM")

            return float(c)

    @classmethod
    def get_OS(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("hardwareReq").get("OS")

            if "linux" in c:
                c = "linux"

            return c.lower()

    @classmethod
    def get_netProt_type(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("networkReq").get("protocolType")

            return c.lower()

    @classmethod
    def get_netProt_ver(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("networkReq").get("Version")

            return c


    @classmethod
    def get_softReq(cls, path):
            
            y = Application.parse_sysreq(path)
            
            c = y.get("app").get("softwareReq")

            return c

