package eu.netide.core.globalfib;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.TCP;
import org.projectfloodlight.openflow.types.MacAddress;

/**
 * Created by arne on 17.09.15.
 */
public class Utils {

    static org.onlab.packet.MacAddress OF4jMacToPMac(MacAddress mac)
    {
        return org.onlab.packet.MacAddress.valueOf(mac.getBytes());
    }

    public static IPv4 getIPv4FromEth(Ethernet eth) {
        return getProtoFromEth(eth, IPv4.class);
    }

    public static <T> T getProtoFromEth(Ethernet eth, Class<T> clazz) {
        IPacket payload = eth;
        while (payload !=null) {
            if (clazz.isInstance(payload))
                return (T) payload;
            payload = payload.getPayload();
        }
        return null;
    }



}
