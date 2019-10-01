import java.net.InetAddress;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Paxos {
    Map<Integer, Node<String> > nodeList;
    private static Integer numNodes = 0;

    Paxos(){
        nodeList = new HashMap<>();
    }
    Communication comm = Communication.getInstance(); // Object to send messages
    Node<String> addNode(InetAddress ip, Integer sendPort, Integer recPort) throws IOException {
        Node<String> node = new Node<>(numNodes);
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

        Node<String> n1=paxos.addNode(localIp, 7090, 7091);
        Node<String> n2=paxos.addNode(localIp, 7100, 7092);
        n1.start();
        System.out.println("temp");
        n2.start();
        // paxos.addNode(localIp, 7080);
        
    }
}