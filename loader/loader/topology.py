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
     Antonios Manousis, <antonios.manousis@intel.com>
     Gregor Best,       <gbe@mail.upb.de>
"""

# TODO:
# [ ] Add support for other controllers:
#     [ ] Ryu
#     [ ] POX
#     [ ] Floodlight
# [ ] Determine correct controller to use
#     - Try them one after another until one works? Could be unreliable.

import json
import logging
import sys

import requests
from requests.auth import HTTPBasicAuth

ODL_URL_EDGES = 'http://{addr}/controller/nb/v2/topology/{container}'
ODL_URL_NODES = 'http://{addr}/controller/nb/v2/switchmanager/{container}/nodes'

containerName = 'default'

# Alternative URL schema:
# /controller/nb/v2/topology/default
# /controller/nb/v2/switchmanager/default/nodes

def getSWID(id):
    return id.replace(":", "")

# "83.212.118.90:8080"
def get(addr="127.0.0.1:8080"):
    r = requests.get(ODL_URL_EDGES.format(addr=addr, container=containerName),
            auth=HTTPBasicAuth('admin', 'admin'))
    topo_info = r.json()
    # logging.debug(json.dumps(topo_info, indent=2))
    odlEdges = topo_info['edgeProperties']
    switches = {}
    edges = []
    cores = {}
    hosts = {}
    for edge in odlEdges:
        n1 = getSWID(edge['edge']['tailNodeConnector']['node']['id'])
        n2 = getSWID(edge['edge']['headNodeConnector']['node']['id'])
        n1_inf = edge['edge']['tailNodeConnector']['id']
        n2_inf = edge['edge']['headNodeConnector']['id']
        if not n1 in switches:
            switches[n1] = {}
            switches[n1][n2] = n1_inf
            if not n2 in switches:
                switches[n2] = {}
            switches[n2][n1] = n2_inf
        edges.append({"from": n1, "to": n2})

    # `switches` is a dictionary with switches as keys and a dict that maps neighbors to ports as values
    # `edges` is a list of dicts with `to` and `from` keys that indicate edge endings
    return json.dumps({ "switches": switches, "edges": edges }, indent=2)
