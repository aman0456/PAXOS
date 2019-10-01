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
    Integer cur_id; // The Id to which it promised
    V pre_acc; // value accepted after promised
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
        ArrayList <Integer> sendnodes = new ArrayList<>();  // ASSUME:
        for(Integer i : sendnodes){
            comm.send(prepare_msg, nodeId, i);
            debug("Sent Prepare to node " + Integer.toString(i));
        }
        
    }
    void promise(Integer prop_id) throws IOException {
        // Needs <ID
        Integer rec_Nodeid = 0;                 // ASSUME: Nodeid from which the prop_id is received
        String promise_msg = "promise";
        if(this.isPromised){
            if(prop_id.compareTo(this.cur_id) < 0){
                // Ignore or send NACK with cur_id 
                comm.send_nack(rec_Nodeid);
            } else if(prop_id.compareTo(this.cur_id) == 0){
                    
            } else{
                if(isAccepted){
                    // [Edit/Send] promise_msg with prop_id 
                    comm.send(promise_msg, nodeId, rec_Nodeid);
                }else{
                    // [Send] promise_msg with prop_id 
                    cur_id = prop_id;
                    comm.send(promise_msg, nodeId, rec_Nodeid);
                }
            }
              
        }else{
            
            // [Send] promise_msg with prop_id  
            this.isPromised = true;
            comm.send(promise_msg, nodeId, rec_Nodeid);
        }
        debug("Promise sent to " + Integer.toString(rec_Nodeid));
    }

    // This value is set by the driver function, pre_acc
    void accept_request(Integer id, V value) throws IOException {
        String accept_request_msg = "accept_request";
        // Integer prop_id  ;                                      // ASSUME: To use to create prepare_msg
        ArrayList <Integer> promisenodes = new ArrayList<>();  // ASSUME: promisenodes are stored 
        for(Integer i : promisenodes){
            comm.send(accept_request_msg, nodeId, i);
        }
    }

    void accept(Integer id,V value) throws IOException {
        String listner_msg = "listner";
        ArrayList <Integer> listnernodes = new ArrayList<>();  // ASSUME: promisenodes are stored 
        for(Integer i : listnernodes){
            comm.send(listner_msg, nodeId, i);
        }
        // add to the log of this 
        pre_acc = value;

    }
    
    @Override
    public void run() { 
        debug("Inside Node run " + nodeId);
        try {
            comm.send("Hello Hello", nodeId, 1 - nodeId);
            String rev = comm.receive(nodeId);
            debug("Received : " + rev);
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