################################################################################
# NetIDE Intermediate Protocol - Python implementation                         #
# NetIDE FP7 Project: www.netide.eu, github.com/fp7-netide                     #
# author: Roberto Doriguzzi Corin (roberto.doriguzzi@create-net.org)           #
################################################################################
# Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica           #
# Investigacion Y Desarrollo SA (TID), Fujitsu Technology Solutions GmbH (FTS),#
# Thales Communications & Security SAS (THALES), Fundacion Imdea Networks      #
# (IMDEA), Universitaet Paderborn (UPB), Intel Research & Innovation Ireland   #
# Ltd (IRIIL), Fraunhofer-Institut fur Produktionstechnologie (IPT), Telcaria  #
# Ideas SL (TELCA)                                                             #
#                                                                              #
# All rights reserved. This program and the accompanying materials             #
# are made available under the terms of the Eclipse Public License v1.0        #
# which accompanies this distribution, and is available at                     #
# http://www.eclipse.org/legal/epl-v10.html                                    #
################################################################################
import struct

NETIDE_VERSION = 0x02
NetIDE_Header_Format = '!BBHIIQ'

OPENFLOW_PROTO = 0x11
NETCONF_PROTO = 0x12
OPFLEX_PROTO = 0x13
OPENFLOW_10 = 0x01
OPENFLOW_11 = 0x02
OPENFLOW_12 = 0x03
OPENFLOW_13 = 0x04
OPENFLOW_14 = 0x05
OPENFLOW_15 = 0x06
NETCONF_10 = 0x01
OPFLEX = 0x00

class NetIDEOps:
    NetIDE_version = NETIDE_VERSION
    NetIDE_Header_Size = struct.calcsize(NetIDE_Header_Format)

    #Define the NetIDE message types and codes
    NetIDE_header = {
        'VERSION'             : 0x00,
        'TYPE'                : 0x01,
        'LENGTH'              : 0x02,
        'XID'                 : 0x03,
        'MOD_ID'              : 0x04,
        'DPID'                : 0x05
    }
    
    NetIDE_type = {
        'NETIDE_HELLO'      : 0x01,
        'NETIDE_ERROR'      : 0x02,
        'NETIDE_MGMT'       : 0x03,
        'NETIDE_OPENFLOW'   : OPENFLOW_PROTO,
        'NETIDE_NETCONF'    : NETCONF_PROTO,
        'NETIDE_OPFLEX'     : OPFLEX_PROTO
    }

 #Encode a message in the NetIDE protocol format
    @staticmethod
    def netIDE_encode(type, xid, module_id, datapath_id, msg):
        length = len(msg)
        type_code = NetIDEOps.NetIDE_type[type]
        #if no transaction id is given, generate a random one.
        if xid is None:
            xid = 0
        if module_id is None:
            module_id = 0
        if datapath_id is None:
            datapath_id = 0
        values = (NetIDEOps.NetIDE_version, type_code, length, xid, module_id, datapath_id, msg)
        packer = struct.Struct(NetIDE_Header_Format+str(length)+'s')
        packed_msg = packer.pack(*values)
        return packed_msg

    #Decode NetIDE header of a message (first 16 Bytes of the read message
    @staticmethod
    def netIDE_decode_header(raw_data):
        unpacker = struct.Struct(NetIDE_Header_Format)
        return unpacker.unpack_from(raw_data,0)

    #Decode NetIDE messages received in binary format. Iput: Raw data and length of the encapsulated message
    #Length can be retrieve by decoding the header first
    #NEEDS TO BE CHANGED MAYBE?
    @staticmethod
    def netIDE_decode(raw_data, length):
        unpacker = struct.Struct(NetIDE_Header_Format+str(length)+'s')
        return unpacker.unpack(raw_data)

    #Encode the hello handshake message. protocols is a dictionary such as: {OPENFLOW_PROTO: [1,3], NETCONF_PROTO: []}
    @staticmethod
    def netIDE_encode_handshake(protocols):
        #Get count for number of supported protocols and versions
        count = 0
        values = []
        for protocol in protocols:
            for version in protocols[protocol]:
                values.append(protocol)    #protocol
                values.append(version)    #version
                count+=2

        packer = struct.Struct('!'+str(count)+'B')
        packed = packer.pack(*values)
        return packer.pack(*values)

    #Decode the hello handshake message and return tuple
    @staticmethod
    def netIDE_decode_handshake(raw_data, length):
        packer = struct.Struct('!'+str(length)+'B')
        unpacked = packer.unpack(raw_data)
        return unpacked

    #Return the key name from a value in a dictionary
    @staticmethod
    def key_by_value(dictionary, value):
        for key, val in dictionary.iteritems():
            if value == val:
                return key
