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


    def valid_requirements(self,path):
        #System requirements
        cpu_req = self.get_cpu(path)
        ram_req = self.get_RAM_size(path)
        os_req = self.get_OS(path)
 
        #Actual system
       real_cpu = scpuinfo.get_cpu_info()['hz_actual_raw'][0]/1e6
       if cpu_req < real_cpu:
        return False
       real_RAM = virtual_memory().total/(1024*1024)
       if ram_req < real_RAM:
        return False
       real_op_sys = sys.platform
       if 'linux' in op_sys:
        real_op_sys = 'linux'
       if (os_req!= 'any') and (os_req != real_op_sys):
        return False
       d = dict(os.environ)
       path = d['PATH'].split(':')
       for route in path:
        try:
             ls_result = subprocess.check_output(["ls", route])
        except subprocess.CalledProcessError as e:
             #print e.output
             return False
        if 'ovs-vsctl' in ls_result:
            ovs = subprocess.check_output(["ovs-vsctl", "--version"])
            ovs = ovs.split('\n')[0]
            ovs = ovs.split(' ')
            #TODO compare ovs version to of version required
       #TODO possible NETCONF requirement
       for sw in sw_req:
        flag = False
        for route in path:
        try:
            ls_result = subprocess.check_output(["ls", route])
        except subprocess.CalledProcessError as e:
            #print e.output
            return False
        if sw_req[sw] in ls_result:
         if flag:
          print "Several versions of software: " + sw_req[sw]
         flag = True
         #TODO check if sw version fulfills the requirements
         #try:
              #version = subprocess.Popen([sw_req[sw], "-version"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
         #except subprocess.CalledProcessError as e:
              #print e.output
         #try:
              #version = subprocess.Popen([sw_req[sw], "--version"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
         #except subprocess.CalledProcessError as e:
              #print e.output
        if flag == False:
         return False
       return True

    @classmethod
    def parse_sysreq(cls, path):
        sysreqPath = os.path.basename(path) + ".sysreq"
       
    
        with open(os.path.join(path, sysreqPath)) as f:
            
            s = io.BytesIO()

            st = reduce(lambda x, y: x + y, f)
            f.seek(0)

            enclosed = st.lstrip()[0] == "{"

            if not enclosed: s.write("{")
            prev_line = ''
            sw_count = 0
            for line in f:
                if prev_line == '':
                    new_line = "app\n"
                else:
                    new_line = line.replace(" '", ": '").replace("\t", "  ")

                if "software\n" in prev_line:
                    prev_line = prev_line.replace("\n", str(sw_count)+"\n")
                    sw_count += 1

                if '{' in new_line:
                    prev_line= prev_line.replace("\n", ":\n")
                elif (prev_line != '' and '{' not in prev_line) and '}' not in new_line:
                    prev_line = prev_line.replace("\n", ",\n")
                #print prev_line
                s.write(prev_line)
                prev_line = new_line
            s.write(prev_line)
            if not enclosed: s.write("}")
            
            s.seek(0)
            
            y = load(s)

            return y

    @classmethod
    def get_controller_name(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("controller").get("name")
            
            return c

    @classmethod
    def get_controller(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("controller").get("name")

            return {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c.lower())

    @classmethod
    def get_cpu(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("hardwareReq").get("CPU")

            return c

    @classmethod
    def get_RAM_size(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("hardwareReq").get("RAM")

            return c

    @classmethod
    def get_OS(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("hardwareReq").get("OS")

            if "linux" in c:
                c = "linux"

            return c.lower()

    @classmethod
    def get_netProt_type(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("networkReq").get("protocolType")

            return c

    @classmethod
    def get_netProt_ver(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("networkReq").get("Version")

            return c


    @classmethod
    def get_softReq(self, path):
            
            y = self.parse_sysreq(path)
            
            c = y.get("app").get("softwareReq")

            return c

