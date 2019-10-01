import java.util.ArrayList;
import java.io.IOException;
import java.lang.Thread;

// import 
/**
 * Node
 */
public class Node< V extends Comparable<V>> extends Thread {
    ArrayList<LogEntry<Integer, V>> nodelog;

    Integer nodeId; // Unique Id for each node
    Boolean isPromised; // Did it promise to anyone ?
    Boolean isAccepted;
    Integer curPropId; // The Id to which it promised
    V preAcc; // value accepted after promised
    Communication comm = Communication.getInstance(); // Object to send messages

    Node(Integer nodeId) {
        this.nodelog = new ArrayList<>();
        this.nodeId = nodeId;
    }

    void debug(String str) {
        boolean a = true;
        if(a) System.out.println(str);
    }

    void prepare() throws IOException {
        // Send to all prepare - id
        String prepare_msg = "prepare";
        // Integer prop_id  ;                                      // ASSUME: To use to create prepare_msg
        comm.sendAll(prepare_msg, nodeId);
    }
    void promise(Integer propId) throws IOException {
        // Needs <ID
        Integer recNodeId = 0;                 // ASSUME: Nodeid from which the propId is received
        String promiseMsg = "promise";
        if (this.isPromised) {
            if (propId.compareTo(this.curPropId) <= 0){
                // Ignore or send NACK with curPropId 
                comm.send_nack(nodeId, recNodeId);
            } else {
                if (isAccepted) {
                    // [Edit/Send] promiseMsg with propId 
                } else {
                    // [Send] promiseMsg without propId 
                    curPropId = propId;
                }
                comm.send(promiseMsg, nodeId, recNodeId);
            }
        } else {
            // [Send] promise_msg without prop_id  
            this.isPromised = true;
            this.curPropId = propId;
            comm.send(promiseMsg, nodeId, recNodeId);
        }
        debug("Promise sent to " + Integer.toString(recNodeId));
    }

    // This value is set by the driver function, preAcc
    void accept_request(Integer id, V value) throws IOException {
        String accept_request_msg = "accept_request";
        // Integer prop_id  ;                                      // ASSUME: To use to create prepare_msg
        ArrayList <Integer> promiseNodes = new ArrayList<>();  // ASSUME: promiseNodes are stored 
        for(Integer i : promiseNodes){
            comm.send(accept_request_msg, nodeId, i);
        }
    }

    void accept(Integer id, V value) throws IOException {
        String listenerMsg = "listner";
        Integer recNodeId = 0;                 // ASSUME: Nodeid from which the accept request is received
        if (this.isPromised && id.compareTo(this.curPropId) <= 0) {
        	comm.sendReject(nodeId, recNodeId);
        } else {
        	this.isAccepted = true;
        	this.preAcc = value;
        	comm.sendAll(listenerMsg, nodeId);
        }
    }
    
    @Override
    public void run() { 
        debug("Inside Node run " + nodeId);
        try {
            // comm.send("Hello Hello", nodeId, 1 - nodeId);
            // String rev = comm.receive(nodeId);
            // debug("Received : " + rev);
            if (nodeId == 0) {
            	prepare();
            }
            comm.receive(nodeId);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
 
    // public static void main(String[] args) {
    //     Node<Integer,String> node = new Node<Integer,String>();
    //     node.debug("main called here");
    //     node.debug("main working");
    //     try{Thread.sleep(500);}catch(InterruptedException e){System.out.println(e);}
    //     node.debug("done working");
    // }



}


/* Templates which might be needed 

a.compareTo(b) -- (>0 if a>b ), ( <0 if a<b ), (==0 if a==b)
class Foo<T extends Comparable<T>>{
    T value;
}

public class Entry<T> implements Comparable<Entry<T>>
{
    T value;
    public Integer compareTo(Entry<T> a)
    {
        String temp1 = this.toString();
        String temp2 = a.toString();
        return temp1.compareTo(temp2);
    }
}


*/ 