import argparse
import logging
import re

import mininet.topo
import mininet.topolib
import mininet.util
import mininet.net
import os.path
import xml.dom.minidom as minidom
import xml.etree.ElementTree as ET

BUILTIN_TOPOLOGIES = {
    "minimal": mininet.topo.MinimalTopo,
    "linear": mininet.topo.LinearTopo,
    "reversed": mininet.topo.SingleSwitchReversedTopo,
    "single": mininet.topo.SingleSwitchTopo,
    "tree": mininet.topolib.TreeTopo,
    "torus": mininet.topolib.TorusTopo
}


def generate_xml_file(topology, output_path, print_output=False):
    """Creates an XML representation of the topology and writes it to a file.
    :param topology: Topology to represent
    :param output_path: Path to write to
    :param print_output: If true, write the XML output to stdout
    :return:
    """
    root = ET.Element("Topology")
    root.set("xmlns", "http://netide.eu/schemas/topologyspecification/v1")
    hosts = ET.SubElement(root, "Hosts")
    switches = ET.SubElement(root, "Switches")
    links = ET.SubElement(root, "Links")

    for host in topology.hosts():
        node_info = topology.nodeInfo(host)
        mac_str = ""
        if ":" in node_info["mac"]:
            mac_str = node_info["mac"]
        else:
            i = 0
            for c in node_info["mac"]:
                mac_str += c
                if i % 2 == 1 and i < len(node_info["mac"]) - 1:
                    mac_str += ":"
                i += 1
        ET.SubElement(hosts, "Host", id=host, mac=mac_str, ip=node_info["ip"])

    for switch in topology.switches():
        node_info = topology.nodeInfo(switch)
        ET.SubElement(switches, "Switch", id=switch, dpid=node_info["dpid"])

    for link in topology.links():
        src = link[0]
        dst = link[1]
        (src_port, dst_port) = topology.port(src, dst)
        ET.SubElement(links, "Link",
                      src=src, dst=dst, src_port=str(src_port), dst_port=str(dst_port))

    xml_string = ET.tostring(root, "utf-8")
    dom = minidom.parseString(xml_string)

    if not output_path:
        output_path = "./topology.xml"
    with open(output_path, "w") as writer:
        dom.writexml(writer, addindent="\t", newl="\n")

    if print_output:
        print(dom.toprettyxml())


def read_customs(path):
    """Reads a python file and returns its contents (classes ...)

    :param path: File to read
    :return: Dict of contents
    """
    if not os.path.isfile(path):
        raise Exception("Not a file: %s" % path)
    customs = {}
    execfile(path, customs, customs)
    return customs


def get_topology(topology_string, customs):
    """Creates an instance of the topology class

    :param topology_string: Topology string in Mininet style
    :param customs: Contents of an optionally provided custom python module
    :return: An instance of the topology class
    """
    class_name, args, kwargs = mininet.util.splitArgs(topology_string)

    topology = None
    if class_name in customs:
        topology = mininet.util.buildTopo(customs, topology_string)
    elif class_name in customs["topos"]:
        topology = mininet.util.buildTopo(customs["topos"], topology_string)
    elif class_name in BUILTIN_TOPOLOGIES:
        topology = mininet.util.buildTopo(BUILTIN_TOPOLOGIES, topology_string)

    if not topology:
        raise Exception("Topology class %s not found." % class_name)

    # Assign IP to host if necessary
    assign_ip_address = False
    for host in topology.hosts():
        node_info = topology.nodeInfo(host)
        if "mac" not in node_info:
            logging.warning("Not all hosts have been explicitly assigned a MAC address. "
                            "Aborting.")
            raise Exception("No MAC address specified")
        if "ip" not in node_info:
            logging.warning("Not all hosts have been explicitly assigned an IP address. "
                            "Using MiniNet default assignment. This may lead to errors.")
            assign_ip_address = True
            break

    if assign_ip_address:
        host_index = 1
        for host in topology.hosts():
            ip_base, prefix_len = mininet.util.netParse("10.0.0.0/8")
            ip = mininet.util.ipAdd(host_index, prefixLen=prefix_len, ipBaseNum=ip_base)
            host_index += 1

            node_info = topology.nodeInfo(host)
            node_info["ip"] = ip
            topology.setNodeInfo(host, node_info)

    # Assign DPID to switch if necessary
    dpid_assigned = False
    for node in topology.switches():
        node_info = topology.nodeInfo(node)
        if "dpid" not in node_info:
            dpid_assigned = True
            nums = re.findall(r'\d+', node)
            if nums:
                dpid = hex(int(nums[ 0 ]))[2:]
            node_info["dpid"] = '0' * (16 - len(dpid)) + dpid
            topology.setNodeInfo(node, node_info)
    if dpid_assigned:
        logging.warning("Not all switches have been explicitly assigned a DPID. "
                        "Using MiniNet default assignment. This may lead to errors.")

    return topology


def main():
    parser = argparse.ArgumentParser(
        description="Converts a Mininet topology to a topology file readable by the core.")
    parser.add_argument("--topo", metavar="TOPOLOGY", type=str, required=True,
                        help="Topology to parse.")
    parser.add_argument("--custom", metavar="CUSTOM_PATH", type=str,
                        help="Path to python file with custom topology.")
    parser.add_argument("--out", metavar="OUTPUT_PATH", type=str,
                        help="Path of the newly generated xml file.")
    args = parser.parse_args()

    if not args.topo:
        raise Exception("No topology specified.")

    customs = {}
    if args.custom:
        customs = read_customs(args.custom)

    topology = get_topology(args.topo, customs)
    generate_xml_file(topology, args.out)

if __name__ == "__main__":
    main()
