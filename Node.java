import java.util.ArrayList;
import java.lang.Thread;
// import 
/**
 * Node
 */
public class Node <I extends Comparable<I>,V extends Comparable<V>> {
    ArrayList <LogEntry<I,V> > nodelog;
    
    int Node_id; // Unique Id for each node 
    Boolean isPromised; // Did it promise to anyone ?
    Boolean isAccepted;
    I cur_id;   // The Id to which it promised 
    V pre_acc;  // value accepted after promised
    Communication comm = Communication.getInstance(); // Object to send messages
    

    Node(){  this.nodelog = new ArrayList<>();   }
    void debug(String str){
        System.out.println(str);
    }
    void prepare(){
        // Send to all prepare - id
        String prepare_msg = "prepare";
        // I prop_id  ;                                      // ASSUME: To use to create prepare_msg
        ArrayList <Integer> sendnodes = new ArrayList<>();  // ASSUME:
        for(Integer i : sendnodes){
            comm.send(prepare_msg,i);
            debug("Sent Prepare to node " + Integer.toString(i));
        }
        
    }
    void promise(I prop_id){
        // Needs <ID
        int rec_Nodeid = 0;                 // ASSUME: Nodeid from which the prop_id is received
        String promise_msg = "promise";
        if(this.isPromised){
            if(prop_id.compareTo(this.cur_id) < 0){
                // Ignore or send NACK with cur_id 
                comm.send_nack(rec_Nodeid);
            } else if(prop_id.compareTo(this.cur_id) == 0){
                    
            } else{
                if(isAccepted){
                    // [Edit/Send] promise_msg with prop_id 
                    comm.send(promise_msg,rec_Nodeid);
                }else{
                    // [Send] promise_msg with prop_id 
                    cur_id = prop_id;
                    comm.send(promise_msg,rec_Nodeid);
                }
            }
              
        }else{
            
            // [Send] promise_msg with prop_id  
            this.isPromised = true;
            comm.send(promise_msg,rec_Nodeid);
        }
        debug("Promise sent to " + Integer.toString(rec_Nodeid));
    }

    // This value is set by the driver function, pre_acc
    void accept_request(I id, V value){
        String accept_request_msg = "accept_request";
        // I prop_id  ;                                      // ASSUME: To use to create prepare_msg
        ArrayList <Integer> promisenodes = new ArrayList<>();  // ASSUME: promisenodes are stored 
        for(Integer i : promisenodes){
            comm.send(accept_request_msg,i);
        }
    }

    void accept(I id,V value){
        String listner_msg = "listner";
        ArrayList <Integer> listnernodes = new ArrayList<>();  // ASSUME: promisenodes are stored 
        for(Integer i : listnernodes){
            comm.send(listner_msg,i);
        }
        // add to the log of this 
        pre_acc = value;

    }
    // void send_listner(){

    // }

    public static void main(String[] args) {
        Node<Integer,String> node = new Node<Integer,String>();
        node.debug("main called here");
        node.debug("main working");
        try{Thread.sleep(500);}catch(InterruptedException e){System.out.println(e);}
        node.debug("done working");
    }



}


/* Templates which might be needed 

a.compareTo(b) -- (>0 if a>b ), ( <0 if a<b ), (==0 if a==b)
class Foo<T extends Comparable<T>>{
    T value;
}

public class Entry<T> implements Comparable<Entry<T>>
{
    T value;
    public int compareTo(Entry<T> a)
    {
        String temp1 = this.toString();
        String temp2 = a.toString();
        return temp1.compareTo(temp2);
    }
}


*/ 