import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Communication
 */
// Make a singleton class
public class Communication {
    private static Communication comm = null;
    static Integer numNodes = 0;
    Map<Integer, InetAddress> nodesIp;
    Map<Integer, Integer> nodesPort;
    Map<Integer, DatagramSocket> sendingSocket;
    Map<Integer, DatagramSocket> receivingSocket;

    private Communication() {
        nodesIp = new HashMap<>();
        nodesPort = new HashMap<>();
        sendingSocket = new HashMap<>();
        receivingSocket = new HashMap<>();

    }

    public static Communication getInstance() {
        if (comm == null)
            comm = new Communication();
        return comm;
    }

    public void addNode(Integer id, InetAddress ip, Integer port) throws IOException {
        debug("addNode : " + id + " " + ip.toString() + " " + port);
        nodesIp.put(id, ip);
        nodesPort.put(id, port);
        sendingSocket.put(id, new DatagramSocket(port, ip));
        receivingSocket.put(id, new DatagramSocket(port+1, ip));
    }

    public void send(String str, Integer senderId, Integer receiverId) throws IOException {
        debug("Node sending " + senderId + " " + receiverId + " Msg : " + str);
        DatagramPacket DpSend = new DatagramPacket(str.getBytes(), str.length(), nodesIp.get(receiverId),
                nodesPort.get(receiverId)+1);

        sendingSocket.get(senderId).send(DpSend);
        debug("Node sent " + senderId + " " + receiverId + ":"+ (nodesPort.get(receiverId)+1) + " Msg : " + str );

    }

    public String receive(Integer receiverId) throws IOException {
        String rec = null;
        debug("Node receiving " + receiverId + ":" + receivingSocket.get(receiverId).getLocalPort());
        byte[] receive = new byte[65535];
        DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);
        receivingSocket.get(receiverId).receive(DpReceive);
        rec = new String(receive, StandardCharsets.UTF_8);
        debug("Node received " + receiverId + " Msg : " + rec);
        return rec;
    }

    public void send_nack(Integer senderId, Integer receiverId) throws IOException {
        send("nack", senderId, receiverId);
    }

    public void sendReject(Integer senderId, Integer receiverId) throws IOException {
        send("reject", senderId, receiverId);
    }

    public void sendAll(String msg, Integer senderId) throws IOException {
        for (int i = 0; i < numNodes; i++) {
            send(msg, senderId, i);
        }
    }

    void debug(String str) {
        System.out.println(str);
    }

}
// Communication commun = new Communication();
