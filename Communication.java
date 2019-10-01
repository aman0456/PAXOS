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
    Map<Integer, Integer> sendingPort;
    Map<Integer, Integer> receivingPort;
    Map<Integer, DatagramSocket> sendingSocket;
    Map<Integer, DatagramSocket> receivingSocket;

    void debug(String str) {
        boolean a = true;
        if(a) System.out.println(str);
    }

    private Communication() {
        nodesIp = new HashMap<>();
        sendingPort = new HashMap<>();
        sendingSocket = new HashMap<>();
        receivingPort = new HashMap<>();
        receivingSocket = new HashMap<>();

    }

    public static Communication getInstance() {
        if (comm == null)
            comm = new Communication();
        return comm;
    }

    public void addNode(Integer id, InetAddress ip, Integer sendPort, Integer recPort)throws IOException {
        debug("addNode : " + id + " " + ip.toString() + " sendPort:" + sendPort + " recPort:" + recPort);
        nodesIp.put(id, ip);
        sendingPort.put(id, sendPort);
        receivingPort.put(id, recPort);
        sendingSocket.put(id, new DatagramSocket(sendPort, ip));
        receivingSocket.put(id, new DatagramSocket(recPort, ip));
    }

    public void removeNode(Integer id){
        sendingSocket.get(id).close();
        receivingSocket.get(id).close();

        nodesIp.remove(id);
        sendingPort.remove(id);
        receivingPort.remove(id);
        sendingSocket.remove(id);
        receivingSocket.remove(id);
    }

    public void send(String str, Integer senderId, Integer receiverId) throws IOException {
        debug("Node sending " + senderId + " " + receiverId + " Msg : " + str);
        DatagramPacket DpSend = new DatagramPacket(str.getBytes(), str.length(), nodesIp.get(receiverId),receivingPort.get(receiverId));

        sendingSocket.get(senderId).send(DpSend);
        debug("Node sent " + senderId + " " + receiverId + ":"+ receivingPort.get(receiverId) + " Msg : " + str );

    }

    public String receive(Integer receiverId) throws IOException {
        String rec = null;
        debug("Node receiving " + receiverId + ":" + receivingSocket.get(receiverId).getLocalPort());

        byte[] receive = new byte[65535];
        DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);
        receivingSocket.get(receiverId).receive(DpReceive);
        rec = new String(receive, StandardCharsets.UTF_8);
        debug("Node received " + receiverId + " Msg : " + rec );
        
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

    
    public void closeAll(){
        for (Map.Entry<Integer, Integer> pair : sendingPort.entrySet()) {
            removeNode(pair.getKey());
        }
    }

}
// Communication commun = new Communication();
