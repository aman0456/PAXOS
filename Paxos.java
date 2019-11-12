import java.net.InetAddress;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Paxos {
    Map<Integer, Node > nodeList;
    private static Integer numNodes = 0;
    
    Paxos(){
        nodeList = new HashMap<>();
    }
    Communication comm = Communication.getInstance(); // Object to send messages
    Node addNode(InetAddress ip, Integer sendPort, Integer recPort) throws IOException {
        Node node = new Node(numNodes); // CHANGE: TODO:
        comm.addNode(numNodes, ip, sendPort, recPort);
        Communication.numNodes++;
        numNodes++;
        return node;
    }
    
    public static void main(String[] args) throws IOException {
        InetAddress localIp = InetAddress.getLocalHost();
        Paxos paxos = new Paxos();
        Scanner inputReader = new Scanner(System.in);
        
        // ServerSocket s1 = new ServerSocket(0);
        // ServerSocket s2 = new ServerSocket(0);
        // Integer p1 =  s1.getLocalPort();
        // Integer p2 =  s2.getLocalPort();
        // s1.close();
        // s2.close();
        String cmdSet = ">> ";
        System.out.println( "Enter the total number of Nodes: " );
        System.out.print(cmdSet);
        int nn = inputReader.nextInt();
        ArrayList<Node> nodeList = new ArrayList<>();
        // int nn = 5;
        for (int i = 0; i < nn; i++) {
            Node n1=paxos.addNode(localIp, 7000+2*i, 7001+2*i);
            nodeList.add(n1);
        }
        for (int i = 0; i < nn; i++) {
            nodeList.get(i).start();
        }

        
        // paxos.comm.send("TIMY", 0, 2);
        // paxos.comm.send("TIMY", 0, 3);
        // paxos.comm.send("TIMY", 0, 4);

        // paxos.comm.send("CMDPREPARE:50", 0, 1);
        // paxos.comm.send("TIMY:", 0, 1);
        // paxos.comm.send("CMDPREPARE:51", 0, 2);
        // paxos.comm.send("CMDPREPARE:52", 0, 3);
        // paxos.comm.send("CMDPREPARE:53", 0, 4);
        // paxos.addNode(localIp, 7080);
        
        String cmd ;
        String[] cmdList ; 
        String mainCmd;

        Integer value,toId,fromId;
        while(true){
            System.out.print(cmdSet);
            cmd = inputReader.nextLine();
            cmdList = cmd.trim().split(" ");
            mainCmd = cmdList[0];

            if (mainCmd.compareTo("exit") == 0){
                System.exit(0);
            }
            else if (mainCmd.compareTo("cli") == 0){
                    value = Integer.parseInt(cmdList[1]);
                    toId = Integer.parseInt(cmdList[2]);
                    paxos.comm.send("CMDPREPARE:"+value, 0, toId);
            }
            else if (mainCmd.compareTo("timout") == 0){
                value = Integer.parseInt(cmdList[1]);
                toId = Integer.parseInt(cmdList[2]);
                paxos.comm.send("TIMY:"+value, 0, toId);
            }else if (mainCmd.compareTo("") == 0){
                continue;
            } 
            else 
            {
                System.out.println(mainCmd + " : Command not Found");
            }
        }
    }
}