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

# Stuff that checks the environemnt: hardware, languages (?), network (?)

import inspect
import logging
import os
import re
import subprocess

from loader import controllers

class HardwareCheckException(Exception): pass

def check_hardware(h):
    "Raises an exception if any of the requirements in req are not met by the current hardware. \
     Raises a KeyError if an unknown check is requested"

    # Check CPU
    if "cpuarch" in h:
        m = os.uname().machine
        a = h["cpuarch"]
        if a == "x86" and m == "x86_64":
            a = "x86_64"
        if m != a:
            raise HardwareCheckException("Invalid CPU architecture: want {}, have {}".format(a, m))

    if "cpufreq" in h:
        with open("/proc/cpuinfo") as f:
            for l in f.readlines():
                if not l.startswith("cpu MHz"):
                    continue
                have = float(l.split(":", 2)[1])
                if have < h["cpufreq"]:
                    msg = "Invalid CPU frequency: want {} MHz, have {} MHz".format(h["cpufreq"], have)
                    raise HardwareCheckException(msg)

    if "cpucores" in h:
        numcpu = 0
        with open("/proc/cpuinfo") as f:
            for l in f.readlines():
                if not l.startswith("processor"):
                    continue
                numcpu += 1
        if numcpu < h["cpucores"]:
            msg = "Invalid number of CPUs: want {}, have {}".format(h["cpucores"], numcpu)
            raise HardwareCheckException(msg)

    # Check Memory
    if "memory" in h:
        with open("/proc/meminfo") as f:
            for l in f.readlines():
                if not l.startswith("MemTotal:"):
                    continue
                l = l.split(":", 1)[1].strip()
                have = int(l.split(" ", 1)[0]) // 1024
                if have < h["memory"]:
                    msg = "Invalid amount of memory: want {} MB, have {} MB".format(h["memory"], have)
                    raise HardwareCheckException(msg)

    for k, v in h.items():
        if k not in ["cpuarch", "cpufreq", "cpucores", "memory"]:
            raise KeyError(k)

class LanguageCheckException(Exception):
    msg = "Can't find a matching {what} version. Wanted {want}, got {have}."
    msgdunno = "Don't know how to check for {what} version {want}."

    def __init__(self, what, want, have=None, dunno=False):
        self.what = what
        self.want = want
        self.have = have
        self.dunno = dunno

    def __str__(self):
        if self.dunno:
            return self.msgdunno.format(what=self.what, want=self.want)
        return self.msg.format(what=self.what, want=self.want, have=self.have)


def check_languages(langs):
    for l in langs:
        if l.get("name") not in ["python", "java"]:
            raise LanguageCheckException(l.get("name"), l.get("version"), dunno=True)
        want = str(l.get("version", ""))
        if l["name"] == "python":
            if want.startswith("3"):
                pbin = "python3"
            elif want.startswith("2"):
                pbin = "python2"
            else:
                raise LanguageCheckException("python", want, dunno=True)
            try:
                v = subprocess.check_output([pbin, "--version"]).decode('utf-8').split(" ", 1)[1].strip()
            except FileNotFoundError:
                raise LanguageCheckException("python", want)
        elif l["name"] == "java":
            try:
                v = subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT).decode("utf-8")
            except FileNotFoundError:
                raise LanguageCheckException("java", want)
            v = v.splitlines()[0].split(" ")[-1].strip('"')
        else:
            assert False, "How did I get here?"

        if not v.startswith(want):
            raise LanguageCheckException(l["name"], want, v)

class ControllerCheckException(Exception): pass

def check_controllers(ctrls):
    for c in ctrls:
        cls = {k.lower(): v for k, v in inspect.getmembers(controllers)}.get(c["name"].lower())
        if cls is None:
            logging.warning("Not checking for unknown controller {}".format(c))
            continue
        v = cls.version()
        logging.debug("{} {}".format(v, type(v)))
        if any([x in c["version"] for x in "[]*+"]) and v is not None:
            logging.debug("Using regex matching for version string '{}'".format(c["version"]))
            found = re.search(c["version"], v)
        else:
            logging.debug("Using simple eq check for version string '{}'".format(c["version"]))
            found = v == c["version"]

        if not found:
            raise ControllerCheckException("Expected {} version {}, got {}".format(cls.__name__,
                c["version"], v))
