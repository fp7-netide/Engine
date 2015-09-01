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

RYU_BACKEND_PORT=41414
NETIDE_VERSION = 0x2

#Define the NetIDE Version
NetIDE_version = NETIDE_VERSION
NetIDE_Header_Format = '!BBHIIQ'
NetIDE_Header_Size = struct.calcsize(NetIDE_Header_Format)


class NetIDEOps:

    #Define the NetIDE Version
    NetIDE_version = 0x01
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
        'NETIDE_OPENFLOW'   : 0x11,
        'NETIDE_NETCONF'    : 0x12,
        'NETIDE_OPFLEX'     : 0x13
    }

    #Define the supported switch control protocols we support and versios
    #Should be determined by underlying network/switches??
    #Protocol:
    #0x10 = OpenFlow: versions - 0x01 = 1.0; 0x02 = 1.1; 0x03 = 1.2; 0x04 = 1.3; 0x05 = 1.4
    #0x11 = NetConf: versions - 0x01 = RFC6241 of NetConf
    #0x12 = OpFlex: versions - 0x00 = Version in development
    NetIDE_supported_protocols = {
        0x11    : {0x01, 0x02, 0x03, 0x04, 0x5},
        0x12    : {0x01},
        0x13    : {0x00}
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
        return unpacker.unpack(raw_data)

    #Decode NetIDE messages received in binary format. Iput: Raw data and length of the encapsulated message
    #Length can be retrieve by decoding the header first
    #NEEDS TO BE CHANGED MAYBE?
    @staticmethod
    def netIDE_decode(raw_data, length):
        unpacker = struct.Struct(NetIDE_Header_Format+str(length)+'s')
        return unpacker.unpack(raw_data)

    #Encode the hello handshake message
    @staticmethod
    def netIDE_encode_handshake(protocols):

        #Get count for number of supported protocols and versions
        count = 0
        values = []
        for protocol in protocols.items():
            for version in protocol[1]:
                count += 1
                values.append(protocol[0])
                values.append(version)

        packer = struct.Struct('!'+str(count*2)+'B')
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