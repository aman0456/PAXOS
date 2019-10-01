import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.Thread;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class Node extends Thread {
    ArrayList<LogEntry<Integer, String>> nodelog;

    Integer nodeId; // Unique Id for each node
    Boolean isPromised; // Did it promise to anyone ?
    Boolean isAccepted; // Did it accept any value ?
    Boolean isPromiseWait; // Waiting for promise majority
    Boolean isAcceptWait;
    Integer curPropPid; // Id which is prepared
    Integer curPromPid; // The Id to which it promised
    String valueAcc; // value accepted after promised
    Boolean isPromiseMajority; // Did the promises reach the majority
    Boolean isAcceptMajority;

    ArrayList<Integer> promisedIds;
    
    Duration duration = Duration.ofSeconds(10);
    Instant startTime;
    Communication comm = Communication.getInstance(); // Object to send messages

    // Debuging
    PrintWriter writer;
    static Integer ordering;

    Node(Integer nodeId) throws FileNotFoundException, UnsupportedEncodingException {
        this.nodelog = new ArrayList<>();
        this.nodeId = nodeId;
        this.curPropPid = 21;
        this.curPromPid = 51;
        this.ordering = 0;
        this.promisedIds = new ArrayList<>();
        this.isPromiseMajority = false;
        this.isAcceptMajority = false;

        this.isPromised = false;
        this.isAccepted = false;
        this.isAcceptWait = false;
        this.isPromiseWait = false;
        
        writer = new PrintWriter("nodeDebug/"+nodeId,"UTF-8");
    }

    void debug(String str) {
        boolean a = true;
        // if(a) System.out.println(str);
        if(a) writer.println(ordering + ":"+ str);
        ordering++;
        writer.flush();
    }
    // String makeMsg(String delim, String[] params){
    //     String msg = "";
    //     for(String i: params){  msg = msg + delim + i;}
    //     return msg;
    // }
    Boolean checkTimeout(Instant startTime){
        Duration timeElapsed = Duration.between(startTime,Instant.now());
        return timeElapsed.toMillis() > duration.toMillis();
    }

    void prepare() throws IOException {
        /* To send Prepare Msg */
        isPromiseWait = true;
        isAcceptWait = false;
        isAcceptMajority = false;
        isPromiseMajority = false;
        promisedIds.clear();
        startTime = Instant.now();


        debug("Sending Prepare " + curPropPid);
        String prepareMsg = "PREPARE:"+nodeId+":"+curPropPid;
        comm.sendAll(prepareMsg, nodeId);

    }

    
    void promise(Integer propPid, Integer fromID) throws IOException {
        /* sends Response when received Prepare Msg */
        
        String promiseMsg = "PROMISE:"+nodeId+":"+propPid;
        if (this.isPromised) {
            if (propPid.compareTo(this.curPromPid) <= 0){
                debug("Prepare " + propPid + " rejected from " + fromID);
                comm.send_nack(nodeId, fromID); //TODO: For the propID nackMsg
            } else {
                if (isAccepted) {
                    // Sending Promised Msg with value
                    debug("Sending Promise "+ propPid + " to " + fromID + " accepted " + curPromPid + " " + valueAcc);
                    String newpromiseMsg = promiseMsg +":"+curPromPid+":"+ valueAcc;
                    comm.send(newpromiseMsg,nodeId,fromID);
                    curPromPid = propPid;
                } else {
                    // Change the promised node
                    debug("Sending Promise "+ propPid + " to " + fromID);
                    comm.send(promiseMsg,nodeId,fromID);
                    curPromPid = propPid;
                }
            }
        } else {
            this.isPromised = true;
            this.curPromPid = propPid;
            debug("Sending Promise "+ propPid + " to " + fromID);
            comm.send(promiseMsg, nodeId, fromID);
        }
        debug("Promise sent to " + Integer.toString(fromID));
    }

    void accept_request(Integer sendId, String value) throws IOException {
        /* to send the value to the promised nodes. curPropPid can't change in this */
        startTime = Instant.now();

        debug("Sending Accept-Request "+ curPropPid + " " + value); 
        String accept_requestMsg = "ACCEPT_REQUEST:"+nodeId+":"+curPropPid+":"+value;
        comm.send(accept_requestMsg, nodeId, sendId);
    }

    void accept(Integer propId, Integer fromId, String value) throws IOException {
        /* to send accept for the promised pid and value */
        String acceptMsg = "ACCEPT:"+nodeId+":"+curPromPid+":"+value;
        
        if (this.isPromised && propId.compareTo(this.curPromPid) < 0) {
            debug("Rejecting Accept-Request " + propId + " "+ value);
        	comm.sendReject(nodeId, fromId);
        } else {
        	isAccepted = true;
            valueAcc = value;
            debug("Sending Accept " + curPromPid + " "+ value);
        	comm.send(acceptMsg, nodeId, fromId);
        }
    }
    
    @Override
    public void run() { 
        debug("Inside Node run " + nodeId);
        String valueSend = "";
        int acceptCount = 0;
        try {
            
            while(true){
                String cmd = comm.receive(nodeId);
                String[] parList = cmd.split(":");
                if (isPromiseWait){
                    assert !isPromiseMajority;
                    if(checkTimeout(startTime)){
                        debug("Prepare Timeout " + curPropPid + " "+ valueSend);
                        curPropPid++;
                        debug("called at promise");
                        prepare();
                    }
                }
                if (isAcceptWait ){
                    if(checkTimeout(startTime)){
                        debug("Accept Timeout " + curPropPid + " "+ valueSend);
                        debug("called at Accept");
                        curPropPid++;
                        prepare();
                    } 
                }
                if (parList[0].compareTo("TIMEOUT") == 0){
                    // debug();
                    
                }else  
                if (parList[0].compareTo("CMDPREPARE") == 0){
                    // If another request comes in middle of protocol, it rejected 
                    valueSend = parList[1].trim();
                    prepare();
                } else 
                if (parList[0].compareTo("PREPARE") == 0){
                    // debug(nodeId + " " +cmd.toString());
                    // debug(nodeId + " " +parList[1].toString());
                    // debug(nodeId + " " +parList[2].toString());
                    // continue;
                    Integer fromId = Integer.parseInt(parList[1].trim());
                    Integer propPid = Integer.parseInt(parList[2].trim());
                    debug("Received Prepare " + propPid + " from " + fromId);
                    promise(propPid, fromId);
                } else 
                if (parList[0].compareTo("PROMISE") == 0){

                    Integer fromId = Integer.parseInt(parList[1].trim());
                    Integer propPid = Integer.parseInt(parList[2].trim());
                    debug("Received Promise " + fromId + " "+ propPid + " " + curPropPid);
                    if(propPid != curPropPid) continue;
                    promisedIds.add(fromId);
                    
                    // Check Majority If majority then send 
                    if(isPromiseMajority){
                        accept_request(fromId,valueSend);
                    } else {
                        
                        if (comm.checkMajority(promisedIds.size()) ){
                            debug("Reached Majority " + promisedIds.size() + " Promises " + curPropPid);
                            isPromiseMajority = true;
                            assert !isAcceptMajority;
                            acceptCount = 0;
                            for (Integer tosendId : promisedIds){
                                accept_request(tosendId, valueSend);
                            }
                        }
                    }
                } else 
                if (parList[0].compareTo("ACCEPT_REQUEST") == 0){
                    Integer fromId = Integer.parseInt(parList[1].trim());
                    Integer acceptPid = Integer.parseInt(parList[2].trim());
                    String valueAcc = parList[3].trim();
                    accept(acceptPid, fromId, valueAcc);
                } else 
                if (parList[0].compareTo("ACCEPT") == 0){
                    acceptCount++;
                    Integer fromId = Integer.parseInt(parList[1].trim());
                    Integer acceptPid = Integer.parseInt(parList[2].trim());
                    String valueAcc = parList[3].trim(); 

                    if (!isAcceptMajority && comm.checkMajority(acceptCount)){
                        debug("Reached Majority " + acceptCount + " Promises " + curPropPid + " Value: " + valueAcc);
                        isAcceptMajority = true;
                        isAcceptWait = false;
                        //TODO: SEND TO ALL 
                    }
                }
            }
            // if (nodeId == 0) {
            // 	prepare();
            // }
            

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
