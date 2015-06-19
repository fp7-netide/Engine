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

import json
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
    ids = id.split(":")
    swid = ""
    for id in ids:
        swid += id
    return swid

def get(addr="83.212.118.90:8080"):
    r = requests.get(ODL_URL_EDGES.format(addr=addr, container=containerName),
            auth=HTTPBasicAuth('admin', 'admin'))
    topo_info = r.json()
    print(json.dumps(topo_info, indent=2), file=sys.stderr)
    odlEdges = topo_info['edgeProperties']
    switches = {}
    edges = []
    cores = {}
    hosts = {}
    for edge in odlEdges:
        print(edge, file=sys.stderr)
        n1 = getSWID(edge['edge']['tailNodeConnector']['node']['id'])
        #print edge['edge']['tailNodeConnector']['node']['id']
        n2 = getSWID(edge['edge']['headNodeConnector']['node']['id'])
        n1_inf = edge['edge']['tailNodeConnector']['id']
        #print n1_inf
        n2_inf = edge['edge']['headNodeConnector']['id']
        if not n1 in switches:
            switches[n1] = {}
            switches[n1][n2] = n1_inf
            if not n2 in switches:
                switches[n2] = {}
            switches[n2][n1] = n2_inf
        edges.append((n1,n2,1))

    return json.dumps({ "switches": switches, "edges": edges }, indent=2)

if __name__ == "__main__":
    print(get())
