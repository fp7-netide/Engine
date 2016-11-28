package eu.netide.core.globalfib.test;

/**
 * Created by arne on 28.11.16.
 */
public class TestTopologies {
    static String big = "<?xml version=\"1.0\" ?>\n" +
            "<Topology xmlns=\"http://netide.eu/schemas/topologyspecification/v1\">\n" +
            "\t<Hosts>\n" +
            "\t\t<Host id=\"h1\" ip=\"10.0.0.1\" mac=\"00:00:00:00:00:01\"/>\n" +
            "\t\t<Host id=\"h2\" ip=\"10.0.0.2\" mac=\"00:00:00:00:00:02\"/>\n" +
            "\t\t<Host id=\"h3\" ip=\"10.0.0.3\" mac=\"00:00:00:00:00:03\"/>\n" +
            "\t\t<Host id=\"h4\" ip=\"10.0.0.4\" mac=\"00:00:00:00:00:04\"/>\n" +
            "\t\t<Host id=\"h5\" ip=\"10.0.0.5\" mac=\"00:00:00:00:00:05\"/>\n" +
            "\t\t<Host id=\"h6\" ip=\"10.0.0.6\" mac=\"00:00:00:00:00:06\"/>\n" +
            "\t\t<Host id=\"h7\" ip=\"10.0.0.7\" mac=\"00:00:00:00:00:07\"/>\n" +
            "\t\t<Host id=\"h8\" ip=\"10.0.0.8\" mac=\"00:00:00:00:00:08\"/>\n" +
            "\t</Hosts>\n" +
            "\t<Switches>\n" +
            "\t\t<Switch dpid=\"0000000000000001\" id=\"s1\"/>\n" +
            "\t\t<Switch dpid=\"0000000000000002\" id=\"s2\"/>\n" +
            "\t\t<Switch dpid=\"0000000000000003\" id=\"s3\"/>\n" +
            "\t\t<Switch dpid=\"0000000000000004\" id=\"s4\"/>\n" +
            "\t\t<Switch dpid=\"0000000000000005\" id=\"s5\"/>\n" +
            "\t\t<Switch dpid=\"0000000000000006\" id=\"s6\"/>\n" +
            "\t\t<Switch dpid=\"0000000000000007\" id=\"s7\"/>\n" +
            "\t</Switches>\n" +
            "\t<Links>\n" +
            "\t\t<Link dst=\"s4\" dst_port=\"2\" src=\"h8\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s1\" dst_port=\"2\" src=\"h2\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s2\" dst_port=\"1\" src=\"h3\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s5\" dst_port=\"1\" src=\"s1\" src_port=\"3\"/>\n" +
            "\t\t<Link dst=\"s1\" dst_port=\"1\" src=\"h1\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s3\" dst_port=\"2\" src=\"h6\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s4\" dst_port=\"1\" src=\"h7\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s2\" dst_port=\"2\" src=\"h4\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s3\" dst_port=\"1\" src=\"h5\" src_port=\"0\"/>\n" +
            "\t\t<Link dst=\"s6\" dst_port=\"1\" src=\"s3\" src_port=\"3\"/>\n" +
            "\t\t<Link dst=\"s7\" dst_port=\"1\" src=\"s5\" src_port=\"3\"/>\n" +
            "\t\t<Link dst=\"s5\" dst_port=\"2\" src=\"s2\" src_port=\"3\"/>\n" +
            "\t\t<Link dst=\"s7\" dst_port=\"2\" src=\"s6\" src_port=\"3\"/>\n" +
            "\t\t<Link dst=\"s6\" dst_port=\"2\" src=\"s4\" src_port=\"3\"/>\n" +
            "\t</Links>\n" +
            "</Topology>\n";

    static String small = "<?xml version=\"1.0\" ?>\n"+
            "<Topology xmlns=\"http://netide.eu/schemas/topologyspecification/v1\">\n"+
            "\t<Hosts>\n"+
            "\t\t<Host id=\"h1\" ip=\"10.0.0.1\" mac=\"00:00:00:00:00:01\"/>\n"+
            "\t\t<Host id=\"h2\" ip=\"10.0.0.2\" mac=\"00:00:00:00:00:02\"/>\n"+
            "\t</Hosts>\n"+
            "\t<Switches>\n"+
            "\t\t<Switch dpid=\"0000000000000003\" id=\"s3\"/>\n"+
            "\t\t<Switch dpid=\"0000000000000004\" id=\"s4\"/>\n"+
            "\t</Switches>\n"+
            "\t<Links>\n"+
            "\t\t<Link dst=\"s4\" dst_port=\"1\" src=\"s3\" src_port=\"2\"/>\n"+
            "\t\t<Link dst=\"h2\" dst_port=\"0\" src=\"s4\" src_port=\"2\"/>\n"+
            "\t\t<Link dst=\"s3\" dst_port=\"1\" src=\"h1\" src_port=\"0\"/>\n"+
            "\t</Links>\n"+
            "</Topology>\n";
}
