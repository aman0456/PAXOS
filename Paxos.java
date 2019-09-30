import java.net.InetAddress;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.Map;

public class Paxos {

    private static Integer numNodes = 0;
    Communication comm = Communication.getInstance(); // Object to send messages
    Node<Integer, String> addNode(InetAddress ip, Integer port) throws IOException {
        Node<Integer, String> node = new Node<>(numNodes);
        comm.addNode(numNodes, ip, port);
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

        Node<Integer, String> n1=paxos.addNode(localIp,7090);
        Node<Integer, String> n2=paxos.addNode(localIp,7100);
        n1.start();
        System.out.println("temp");
        n2.start();
        // paxos.addNode(localIp, 7080);
        
    }
}