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

class Controller(object):
    name = None
    params   = None

    def version(self):
        return None

    def install(self):
        raise NotImplementedError()

    def start(self):
        raise NotImplementedError()

class RyuController(Controller):
    name = "ryu"
    params = "--ofp-tcp-listen-port={}"

    def __init__(self, port, entrypoint):
        self.port = port
        self.entrypoint = entrypoint

    def __str__(self):
        return 'RyuController(port={}, entrypoint={})'.format(self.port, self.entrypoint)

    def install(self):
        # TODO?
        pass

    def version(self):
        """ Returns either the version of the controller as a string or None if the controller is not installed"""
        try:
            v = subprocess.check_output(["ryu", "--version"], stderr=subprocess.STDOUT).decode("utf-8")
            return v.strip().split(" ", 1)[1]
        except subprocess.CalledProcessError:
            return None

    def start(self):
        cmdline = ["sudo", "ryu-manager", self.params.format(self.port)]
        cmdline.append(self.entrypoint)
        print('Launching "{}" now'.format(cmdline))
        return subprocess.Popen(cmdline).pid
