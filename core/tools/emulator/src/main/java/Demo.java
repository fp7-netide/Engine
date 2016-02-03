import eu.netide.core.api.Constants;
import eu.netide.lib.netip.*;
import org.json.JSONObject;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by timvi on 10.09.2015.
 */
public class Demo implements Runnable {

    private static OpenFlowMessage packetIn;
    private static OpenFlowMessage flowmod;
    private static final String STOP_COMMAND = "Control.STOP";
    private static final String CONTROL_ADDRESS = "inproc://DemoControl";
    private static String id;
    private static int port;
    private ZMQ.Context context;
    private int socketType;

    static {
        packetIn = new OpenFlowMessage();
        Ethernet ethernet = new Ethernet();
        ethernet.setEtherType(Ethernet.TYPE_IPV4);
        ethernet.setSourceMACAddress(MacAddress.valueOf(12345));
        ethernet.setDestinationMACAddress(MacAddress.valueOf(123456));
        IPv4 iPv4 = new IPv4();
        iPv4.setProtocol(IPv4.PROTOCOL_TCP);
        iPv4.setSourceAddress("192.168.1.1");
        iPv4.setDestinationAddress("192.168.1.2");
        ethernet.setPayload(iPv4);
        TCP tcp = new TCP();
        tcp.setDestinationPort((short) 80);
        tcp.setSourcePort((short) 1234);
        iPv4.setPayload(tcp);
        OFPacketIn ofPacketIn = OFFactories.getFactory(OFVersion.OF_10).buildPacketIn().setInPort(OFPort.of(1)).setReason(OFPacketInReason.NO_MATCH).setData(ethernet.serialize()).build();
        packetIn.setOfMessage(ofPacketIn);
        packetIn.setHeader(NetIPUtils.StubHeaderFromPayload(packetIn.getPayload()));
        packetIn.getHeader().setMessageType(MessageType.OPENFLOW);
        packetIn.getHeader().setTransactionId(0);

        flowmod = new OpenFlowMessage();
        OFFactory fact = OFFactories.getFactory(OFVersion.OF_10);
        Match match1 = fact.buildMatch()
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
                .setExact(MatchField.TCP_DST, TransportPort.of(80)).build();
        OFFlowMod offm1 = fact.buildFlowModify()
                .setActions(Stream.of(fact.actions().setTpDst(TransportPort.of(123))).collect(Collectors.toList()))
                .setMatch(match1)
                .build();
        flowmod = new OpenFlowMessage();
        flowmod.setOfMessage(offm1);
        flowmod.setHeader(NetIPUtils.StubHeaderFromPayload(flowmod.getPayload()));
        flowmod.getHeader().setMessageType(MessageType.OPENFLOW);
        flowmod.getHeader().setModuleId(0);
        flowmod.getHeader().setDatapathId(0);
        flowmod.getHeader().setTransactionId(0);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Demo demo = new Demo();
        demo.main();
    }

    public void main() throws IOException, InterruptedException {
        System.out.println("Demo started.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("How do you want to identify? (shim, backendX; default=shim)\r\n> ");
        id = br.readLine();
        if (id.isEmpty()) id =  Constants.SHIM;
        System.out.print("To which port do you want to connect? (default=5555)\r\n> ");
        String portString = br.readLine();
        if (portString.isEmpty()) portString = "5555";
        port = Integer.parseInt(portString);
        boolean set = false;
        while (!set) {
            System.out.print("Which socket type do you want to use? (dealer,sub;default=dealer)\r\n> ");
            String typeString = br.readLine();
            set = true;
            if (typeString.isEmpty() || typeString.equals("dealer")) socketType = ZMQ.DEALER;
            else if (typeString.equals("sub")) socketType = ZMQ.SUB;
            else {
                set = false;
                System.out.println("Invalid selection.");
            }
        }
        context = ZMQ.context(1);
        Thread thread = new Thread(this);
        thread.start();
        while (true) {
            System.out.println("Enter command: (packetIn (p), composition (c), announcement (a), acknowledge (ack), flowmod (f), requestend (r), exit)");
            System.out.print("> ");
            String command = br.readLine();
            if (command.equals("exit")) {
                System.out.println("Exiting...");
                ZMQ.Socket socket = context.socket(ZMQ.PUSH);
                socket.connect(CONTROL_ADDRESS);
                socket.send(STOP_COMMAND);
                socket.close();
                try {
                    thread.join();
                    context.term();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            } else if (command.equals("packetIn") || command.equals("p")) {
                ZMsg msg = new ZMsg();
                msg.add(id);
                msg.add("");
                msg.add(packetIn.toByteRepresentation());
                ZMQ.Socket socket = context.socket(ZMQ.PUSH);
                socket.connect(CONTROL_ADDRESS);
                msg.send(socket);
                socket.close();
                System.out.println("Sent packetIn message.");
            } else if (command.equals("composition") || command.equals("c")) {
                System.out.print("Which file?\r\n> ");
                String pathString = br.readLine();

                Path path = Paths.get(pathString).toAbsolutePath();
                String spec = new String(Files.readAllBytes(path));

                System.out.print("To which port? (default=5556)\r\n> ");
                String mportString = br.readLine();
                if (mportString.isEmpty()) mportString = "5556";
                int mport = Integer.parseInt(mportString);

                System.out.println("Sending composition specification '" + path + "' to management interface at localhost:" + mport + "...");
                ZMQ.Socket msocket = context.socket(ZMQ.DEALER);
                msocket.connect("tcp://localhost:" + mport);
                JSONObject config = new JSONObject()
                        .put("command", "set-configvalue")
                        .put("parameters", new JSONObject()
                                .put("pid", "eu.netide.core.caos")
                                .put("key", "compositionSpecification")
                                .put("value", spec));

                System.out.println("Sending:\r\n" + config.toString(3));

                ManagementMessage msg = new ManagementMessage();
                msg.setPayloadString(config.toString());
                msg.setHeader(NetIPUtils.StubHeaderFromPayload(msg.getPayload()));
                msg.getHeader().setMessageType(MessageType.MANAGEMENT);
                ZMsg zMsg = new ZMsg();
                zMsg.add(id);
                zMsg.add("");
                zMsg.add(msg.toByteRepresentation());
                zMsg.send(msocket);
                msocket.close();
                System.out.println("Sent.");
            } else if (command.equals("announcement") || command.equals("a")) {
                System.out.print("Which moduleName? (default=fw)\r\n> ");
                String moduleName = br.readLine();
                if (moduleName.isEmpty()) moduleName = "fw";

                ModuleAnnouncementMessage announcementMessage = new ModuleAnnouncementMessage();
                announcementMessage.setModuleName(moduleName);
                announcementMessage.setHeader(NetIPUtils.StubHeaderFromPayload(announcementMessage.getPayload()));
                announcementMessage.getHeader().setMessageType(MessageType.MODULE_ANNOUNCEMENT);

                ZMsg zMsg = new ZMsg();
                zMsg.add(id);
                zMsg.add("");
                zMsg.add(announcementMessage.toByteRepresentation());
                ZMQ.Socket socket = context.socket(ZMQ.PUSH);
                socket.connect(CONTROL_ADDRESS);
                zMsg.send(socket);
                socket.close();
                System.out.println("Sent MODULE_ANNOUNCEMENT message with moduleName '" + moduleName + "'.");
            } else if (command.equals("acknowledge") || command.equals("ack")) {
                System.out.print("Which moduleName? (default=fw)\r\n> ");
                String moduleName = br.readLine();
                if (moduleName.isEmpty()) moduleName = "fw";
                System.out.print("Which moduleId? (default=1)\r\n> ");
                String moduleIdString = br.readLine();
                if (moduleIdString.isEmpty()) moduleIdString = "1";
                int moduleId = Integer.parseInt(moduleIdString);

                ModuleAcknowledgeMessage acknowledgeMessage = new ModuleAcknowledgeMessage();
                acknowledgeMessage.setModuleName(moduleName);
                acknowledgeMessage.setHeader(NetIPUtils.StubHeaderFromPayload(acknowledgeMessage.getPayload()));
                acknowledgeMessage.getHeader().setMessageType(MessageType.MODULE_ACKNOWLEDGE);
                acknowledgeMessage.getHeader().setModuleId(moduleId);

                ZMsg zMsg = new ZMsg();
                zMsg.add(id);
                zMsg.add("");
                zMsg.add(acknowledgeMessage.toByteRepresentation());
                ZMQ.Socket socket = context.socket(ZMQ.PUSH);
                socket.connect(CONTROL_ADDRESS);
                zMsg.send(socket);
                socket.close();
                System.out.println("Sent MODULE_ACKNOWLEDGE message with moduleName '" + moduleName + "' and id '" + moduleIdString + "'.");
            } else if (command.equals("flowmod") || command.equals("f")) {
                System.out.print("Which moduleId? (default=1)\r\n> ");
                String moduleIdString = br.readLine();
                if (moduleIdString.isEmpty()) moduleIdString = "1";
                int moduleId = Integer.parseInt(moduleIdString);

                System.out.print("Which datapathId? (default=0)\r\n> ");
                String datapathIdString = br.readLine();
                if (datapathIdString.isEmpty()) datapathIdString = "0";
                int datapathId = Integer.parseInt(datapathIdString);

                flowmod.getHeader().setModuleId(moduleId);
                flowmod.getHeader().setDatapathId(datapathId);

                ZMsg msg = new ZMsg();
                msg.add(id);
                msg.add("");
                msg.add(flowmod.toByteRepresentation());
                ZMQ.Socket socket = context.socket(ZMQ.PUSH);
                socket.connect(CONTROL_ADDRESS);
                msg.send(socket);
                socket.close();
                System.out.println("Sent flowmod message with moduleId '" + moduleIdString + "'.");
            } else if (command.equals("requestend") || command.equals("r")) {
                System.out.print("Which moduleId? (default=1)\r\n> ");
                String moduleIdString = br.readLine();
                if (moduleIdString.isEmpty()) moduleIdString = "1";
                int moduleId = Integer.parseInt(moduleIdString);

                JSONObject config = new JSONObject()
                        .put("command", "finish-request")
                        .put("parameters", new JSONObject()
                                .put("transactionid", "0"));

                System.out.println("Sending:\r\n" + config.toString(3));

                ManagementMessage msg = new ManagementMessage();
                msg.setPayloadString(config.toString());
                msg.setHeader(NetIPUtils.StubHeaderFromPayload(msg.getPayload()));
                msg.getHeader().setMessageType(MessageType.MANAGEMENT);
                msg.getHeader().setTransactionId(0);
                msg.getHeader().setModuleId(moduleId);
                ZMsg zMsg = new ZMsg();
                zMsg.add(id);
                zMsg.add("");
                zMsg.add(msg.toByteRepresentation());
                ZMQ.Socket socket = context.socket(ZMQ.PUSH);
                socket.connect(CONTROL_ADDRESS);
                zMsg.send(socket);
                socket.close();
                System.out.println("Sent requestend with moduleid '" + moduleIdString + "'.");
            } else {
                System.out.println("Unknown command.");
            }
        }
    }

    @Override
    public void run() {
        ZMQ.Socket socket = context.socket(socketType);
        socket.setIdentity(id.getBytes(ZMQ.CHARSET));
        socket.connect("tcp://localhost:" + port);
        if (socketType == ZMQ.SUB)
            socket.subscribe(new byte[0]);
        System.out.println("Connected to localhost:" + port + " as '" + id + "' using a " + (socketType == ZMQ.DEALER ? "DEALER" : "SUB") + " socket.");

        ZMQ.Socket controlSocket = context.socket(ZMQ.PULL);
        controlSocket.bind(CONTROL_ADDRESS);

        ZMQ.Poller poller = new ZMQ.Poller(2);
        poller.register(socket, ZMQ.Poller.POLLIN);
        poller.register(controlSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(10);
            if (poller.pollin(0)) {
                ZMsg message = ZMsg.recvMsg(socket);
                String senderId = message.getFirst().toString();
                byte[] data = message.getLast().getData();
                Message netipmessage = NetIPConverter.parseConcreteMessage(data);
                System.out.println("Received from '" + senderId + "': " + netipmessage.toString());
            }
            if (poller.pollin(1)) {
                ZMsg message = ZMsg.recvMsg(controlSocket);

                if (message.getFirst().toString().equals(STOP_COMMAND)) {
                    break;
                } else {
                    message.send(socket);
                }
            }
        }
        socket.close();
        controlSocket.close();
    }
}
