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
    cmd  = None
    name = None
    params   = None

    def valid(self):
        raise NotImplementedError()

    def install(self):
        raise NotImplementedError()

    def start(self):
        raise NotImplementedError()

class RyuController(Controller):
    name = "ryu"
    cmd  = "ryu-manager"
    params = "--ofp-tcp-listen-port={}"

    def __init__(self, port, entrypoint):
        self.port = port
        self.entrypoint = entrypoint

    def install(self):
        # TODO?
        pass

    def valid(self):
        # TODO: check if self.cmd exists
        return True

    def start(self):
        cmdline = ["sudo", self.cmd, self.params.format(self.port)]
        cmdline.append(self.entrypoint)
        print('Launching "{}" now'.format(cmdline))
        return subprocess.Popen(cmdline).pid
        # return -1

