import java.net.InetAddress;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Paxos {
    Map<Integer, Node > nodeList;
    private static Integer numNodes = 0;

    Paxos(){
        nodeList = new HashMap<>();
    }
    Communication comm = Communication.getInstance(); // Object to send messages
    Node addNode(InetAddress ip, Integer sendPort, Integer recPort) throws IOException {
        Node node = new Node(numNodes);
        comm.addNode(numNodes, ip, sendPort, recPort);
        comm.numNodes++;
        numNodes++;
        return node;
    }

    public static void main(String[] args) throws IOException {
        InetAddress localIp = InetAddress.getLocalHost();
        Paxos paxos = new Paxos();
        // ServerSocket s1 = new ServerSocket(0);
        // ServerSocket s2 = new ServerSocket(0);
        // Integer p1 =  s1.getLocalPort();
        // Integer p2 =  s2.getLocalPort();
        // s1.close();
        // s2.close();
        ArrayList<Node> nodeList = new ArrayList<>();
        int nn = 10;
        for (int i = 0; i < nn; i++) {
            Node n1=paxos.addNode(localIp, 7000+2*i, 7001+2*i);
            nodeList.add(n1);
        }
        for (int i = 0; i < nn; i++) {
            nodeList.get(i).start();
        }
        paxos.comm.send("CMDPREPARE:50", 0, 1);
        paxos.comm.send("CMDPREPARE:51", 0, 2);
        paxos.comm.send("CMDPREPARE:52", 0, 3);
        paxos.comm.send("CMDPREPARE:53", 0, 4);
        // paxos.addNode(localIp, 7080);
        
    }
}