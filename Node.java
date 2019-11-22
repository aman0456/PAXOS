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
	Boolean isCompleted;
	String curMax = null; // Current Value Max 
	
	Integer acceptCount;

	ArrayList<Integer> promisedIds;
	
	// HARDCODED PARAMS
	Duration mainTimeoutDuration = Duration.ofSeconds(2);
	Duration secondaryTimeoutDuration = Duration.ofSeconds(0); 
	Instant mainStartTime;
	Instant secondaryStartTime;
	Communication comm = Communication.getInstance(); // Object to send messages

	Duration heartBeatDuration = Duration.ofSeconds(1);
	Duration heartBeatTimeoutDuration = Duration.ofSeconds(5);
	Instant heartBeatSent; 
	Instant heartBeatReceived;

    Boolean isLeaderPhase; 

	// Debuging
	PrintWriter writer;
	static Integer ordering;
	Integer level0 = 0; // Important
	Integer level1 = 1; // Less Important
	Integer level2 = 2;


	Node(Integer nodeId) throws FileNotFoundException, UnsupportedEncodingException {
		this.nodelog = new ArrayList<>();
		this.nodeId = nodeId;
		this.curPropPid = 100+nodeId;
		this.curPromPid = 100+nodeId;
		Node.ordering = 0;
		
		
		this.promisedIds = new ArrayList<>();
		this.isPromiseMajority = false;
		this.isAcceptMajority = false;

		this.isPromised = false;
		this.isAccepted = false;
		this.isAcceptWait = false;
		this.isPromiseWait = false;
		this.isCompleted = false;

		this.heartBeatReceived = Instant.now();
		this.heartBeatSent = Instant.now();
		this.isLeaderPhase = false;
		
		this.acceptCount = 0;
		
		writer = new PrintWriter("nodeLog/"+nodeId,"UTF-8");
		
	}

	void debug(String str,Integer level0) {
		Integer Verbosity = 2;
		// if(a) System.out.println(str);
		if(level0 <= Verbosity ){ 
			writer.println(ordering + ":"+ str);
			ordering++;
			writer.flush();
		}
	}
	// String makeMsg(String delim, String[] params){
	//     String msg = "";
	//     for(String i: params){  msg = msg + delim + i;}
	//     return msg;
	// }
	Boolean checkTimeout(Instant startTime, Duration timeoutDuration){
		Duration timeElapsed = Duration.between(startTime, Instant.now());
		return timeElapsed.toMillis() > timeoutDuration.toMillis();
	}

	void heartBeat() throws IOException {
		this.heartBeatSent = Instant.now();
		debug("Sending HeartBeat " + nodeId,level2);
		String heartbeatMsg = "HEARTBEAT:"+nodeId;
		comm.sendAll(heartbeatMsg, nodeId); 
	}

	void increPropPid()throws IOException {
		this.curPropPid = this.curPropPid + comm.getNumNodes();
		// this.curPropPid++;
		// if(this.curPropPid % 100 == 0){
		// 	this.curPropPid += 100*comm.getNumNodes();
		// }
		
	}
	
		void handlePrepareTimeout(String msg) throws IOException {
			// curPropPid++;
			increPropPid();
			debug(msg,level0);
			prepare();
		}
	
		void handleAcceptTimeout(String msg) throws IOException {
			// debug("Timeout while waiting for acceptances " + curPropPid + " "+ valueSend);
			// curPropPid++;
			increPropPid();
			debug(msg,level0);
			prepare();
		}
	
		void handleHeartBeatTimeout(String msg) throws IOException {
			// curPropPid++;
			increPropPid();
			debug(msg,level0);
			this.isLeaderPhase = true;
			prepare();
		}
	// void electLeader() throws IOException {
		// 	debug("Contesting for Leader " + nodeId);
	// 	// Value is the nodeId 
	// 	// comm.sendAll();
	// }
	void resetPrepare() throws IOException {
		this.isPromiseWait = true;
		this.isAcceptWait = false;
		this.isAcceptMajority = false;
		this.isPromiseMajority = false;
		this.acceptCount = 0;
		this.promisedIds.clear();
		this.mainStartTime = Instant.now();

		// FIXME 
		this.isPromised = false;
		// this.isAccepted = false;
		this.isCompleted = false;
	}

	void prepare() throws IOException {
		/* To send Prepare Msg */
		resetPrepare();
		mainStartTime = Instant.now();

		debug("Sending Prepare " + curPropPid,level0);
		String prepareMsg = "PREPARE:"+nodeId+":"+curPropPid;
		comm.sendAll(prepareMsg, nodeId);

	}

	void promise(Integer propPid, Integer fromID) throws IOException {
		/* sends Response when received Prepare Msg */
		
		String promiseMsg = "PROMISE:"+nodeId+":"+propPid;
		if (this.isPromised) {
			if (propPid.compareTo(this.curPromPid) <= 0){
				debug("Prepare <= PID:" + propPid + " PROMPID:" + this.curPromPid+ " rejected from " + fromID,level0);
				comm.send_nack(nodeId, fromID); //TODO: For the propID nackMsg
			} else {
				if (isAccepted) {

					/* CHANGE MULTI PAXOS */

					// Sending Promised Msg with value
					debug("Sending Promise PID:"+ propPid + " to " + fromID + " accepted PROMPID:" + curPromPid + " ValACC: " + valueAcc,level0);
					String newpromiseMsg = promiseMsg +":"+curPromPid+":"+ valueAcc;
					comm.send(newpromiseMsg,nodeId,fromID);
					curPromPid = propPid;
				} else {
					// Change the promised node
					debug("Sending Promise Higher PID:"+ propPid + " to " + fromID,level0);
					comm.send(promiseMsg,nodeId,fromID);
					curPromPid = propPid;
				}
			}
		} else {
			this.isPromised = true;
			this.curPromPid = propPid;
			debug("Sending Promise "+ propPid + " to " + fromID,level0);
			comm.send(promiseMsg, nodeId, fromID);
		}
		// debug("Promise sent to " + Integer.toString(fromID),level0);
	}

	void accept_request(Integer sendId, String value) throws IOException {
		/* to send the value to the promised nodes. curPropPid can't change in this */
		mainStartTime = Instant.now();

		debug("Sending Accept-Request to " + sendId + " curPROPPID:"+ curPropPid + " Val: " + value,level0); 
		String accept_requestMsg = "ACCEPT_REQUEST:"+nodeId+":"+curPropPid+":"+value;
		comm.send(accept_requestMsg, nodeId, sendId);
	}

	void accept(Integer propId, Integer fromId, String value) throws IOException {
		/* to send accept for the promised pid and value */
		String acceptMsg = "ACCEPT:"+nodeId+":"+curPromPid+":"+value;
		
		if (this.isPromised && propId.compareTo(this.curPromPid) < 0) {
			debug("Rejecting Accept-Request with lower PID: " + propId + ", My PROMID: " + this.curPromPid + " Val: "+ value,level0);
			comm.sendReject(nodeId, fromId);
		} else {
			isAccepted = true;
			valueAcc = value;
			curMax = value;
			debug("Sending Accept PROMPID:" + curPromPid + " Val: "+ value + " to " + fromId,level0);
			comm.send(acceptMsg, nodeId, fromId);
		}
	}

	@Override
	public void run() { 
		debug("Inside Node run " + nodeId,level0);
		String valueSend = "";
		try {
			while(true){
				
				String cmd = comm.receive(nodeId);
				String[] parList = cmd.trim().split("\\s*:\\s*");
				String mainCmd = parList[0];
				//checking for promise timeout
				
				if(comm.isLeader(this.nodeId) && checkTimeout(this.heartBeatSent, heartBeatDuration)){
					this.heartBeat();
				}
				
				
				if (!this.isLeaderPhase) {
					// TO elect a leader at the very begining 
					if(comm.isLeader(-1)){
						valueSend = this.nodeId.toString();
						handleHeartBeatTimeout("NODE " + this.nodeId + " STARTING ELECTION");
					}
					// If heartBeat timeouts
					if (checkTimeout(this.heartBeatReceived, heartBeatTimeoutDuration)){
						valueSend = nodeId.toString();
						handleHeartBeatTimeout("HEARTBEAT TIMEOUT NODE " + this.nodeId + " STARTING ELECTION");
					}	
				}
				if (isPromiseWait){
					if(checkTimeout(mainStartTime, mainTimeoutDuration) ||
					(isPromiseMajority && checkTimeout(secondaryStartTime, secondaryTimeoutDuration))) {
						isPromiseWait = false;
						
						if (isPromiseMajority) {
							debug("Reached Majority of " + promisedIds.size() + " Promises for proposal " + curPropPid,level0);
							assert !isAcceptMajority;
							assert acceptCount == 0;
							if (curMax != null) valueSend = curMax;
							curMax = valueSend;

							isAcceptWait = true; // FIX

							for (Integer tosendId : promisedIds){
								accept_request(tosendId, valueSend);
							}
						}
						else {
							// debug("PREPARE TIMEOUT FOR PROPOSOL " + curPropPid,level0);
							handlePrepareTimeout("PREPARE TIMEOUT : Pid " + curPropPid + " curVal :"+ curMax);
						}
					}
				}
				
				//checking for accept timeout
				if (isAcceptWait){
					assert !isAcceptMajority;
					if(checkTimeout(mainStartTime,mainTimeoutDuration)) handleAcceptTimeout("Accept Timeout  Pid :" + curPropPid + " curVal :"+ curMax);
				}
				
				/* **************** Asserts ****************************/ 
				
				assert (isPromiseMajority || isAcceptWait || isAcceptMajority) : "Accept assertion failed";
				// assert ();


				/* *****************************************************/ 
				
				if (mainCmd.compareTo("TIMY") == 0) {
					Integer seconds = Integer.parseInt(parList[1]);
					debug("SLEEPING NOW :(",level0);
					String dyingLeaderMsg = "LEADERDEAD";
					comm.resetLeader();
					comm.sendAll(dyingLeaderMsg, this.nodeId);
					
					sleep(seconds*1000);
					// 
				} 
				else if ( mainCmd.compareTo("LEADERDEAD") == 0){
					debug("Leader dead staring paxos",level0);
					this.isLeaderPhase = true;
					// this.curMax = this.nodeId;
					this.valueAcc = null;
					this.curMax = null;
					this.isAccepted = false; 
					valueSend = this.nodeId.toString();
					sleep(100);
					prepare();
					
				} 
				else if (!this.isLeaderPhase && mainCmd.compareTo("CMDPREPARE") == 0){ // TODO: the condition
					// If another request comes in middle of protocol, it rejected 
					// TODO; -- The change for the multi Paxos
					
					valueSend = parList[1];
					// prepare();
					if(comm.isLeader(this.nodeId)){
						
						debug("CURRENT LEADER: " + comm.getLeader() + "WRITING TO LOG VALUE: " + valueSend + " nodeId :" + this.nodeId ,level0);
						// TODO: Write in log 
					}else{
						comm.send(cmd, this.nodeId, comm.getLeader());
					}

				}

				else if (mainCmd.compareTo("PREPARE") == 0){
					Integer fromId = Integer.parseInt(parList[1]);
					Integer propPid = Integer.parseInt(parList[2]);
					debug("Received Prepare " + propPid + " from " + fromId,level0);
					promise(propPid, fromId);
				}

				else if (mainCmd.compareTo("PROMISE") == 0){

					Integer fromId = Integer.parseInt(parList[1]);
					Integer propPid = Integer.parseInt(parList[2]);
					Boolean hasReceiverAccepted = parList.length > 3;
					
					if (propPid.compareTo(curPropPid) != 0) {
						// debug("Ignoring promise for different proposal ",level0);
						debug("Ignoring promise for different proposal "+  " curPROPID:" + curPropPid + " PROPID:" + propPid + " " + (propPid != curPropPid) +" from " + fromId,level0);
						continue;
					}
					// IF THE promise ID is from the lesser ID then the one it has currently promised To 
					promisedIds.add(fromId);
					
					if (hasReceiverAccepted) {
						Integer propIdAccepted = Integer.parseInt(parList[3]);
						String valueAccepted = parList[4];
						debug("Received Promise " + propPid + " " + curPropPid + " from " + fromId + " WITH VALUE; PRE: " + curMax + " CUR: " + valueAccepted,level0);
						
						if (curMax == null) curMax = valueAccepted;
						else {
							curMax = (curMax.compareTo(valueAccepted) > 0 ? curMax : valueAccepted);
						}
						debug("Value Now: " + curMax, level0);
					}else{
						debug("Received Promise " + propPid + " " + curPropPid + " from " + fromId,level0);
					}
					
					// Check Majority If majority then send 
					if (!isPromiseMajority && isPromiseWait){
						if (comm.checkMajority(promisedIds.size()) ){
							isPromiseMajority = true;
							secondaryStartTime = Instant.now();
						}
					}
				}

				else if (mainCmd.compareTo("ACCEPT_REQUEST") == 0){
					Integer fromId = Integer.parseInt(parList[1]);
					Integer acceptPid = Integer.parseInt(parList[2]);
					String valueAccRei = parList[3];
					accept(acceptPid, fromId, valueAccRei);
				}

				else if (mainCmd.compareTo("ACCEPT") == 0){
					acceptCount++;
					Integer fromId = Integer.parseInt(parList[1]);
					Integer acceptPid = Integer.parseInt(parList[2]);
					String valueAccRei = parList[3]; 
					debug("Got Acceptance PROPID:" + curPropPid + " ValACC: " + valueAccRei + " from " + fromId,level0 );
					
					if (acceptPid.compareTo(curPropPid) != 0){
						debug("Ignoring acceptance for different accept request" +  " curPROPID:" + curPropPid + " ACCPID:" + acceptPid + " ValS:" + valueAccRei + " from " + fromId,level0);
						continue;
					}

					if (!isAcceptMajority && comm.checkMajority(acceptCount)){
						assert (curMax.compareTo(valueAcc) == 0);
						isCompleted = true;
						debug("Reached Majority " + acceptCount + " Acceptances " + curPropPid + " Value: " + valueAcc,level0);
						isAcceptMajority = true;
						isAcceptWait = false;
						debug("CURRENT LEADER ELECTED : " + curMax,level0);
						comm.setLeader(Integer.parseInt(curMax));
						this.isLeaderPhase = false;
						this.heartBeatReceived = Instant.now();
						//TODO: SEND TO ALL
					}
				} 
				else if (this.isLeaderPhase){
						// Ignoring other msgs in this
				}
				else if (mainCmd.compareTo("HEARTBEAT") == 0 ){
					Integer fromId = Integer.parseInt(parList[1]);
					if(comm.isLeader(fromId)){
						debug("RECEIVED HEARTBEAT FROM LEADER : " + fromId,level2);
						this.heartBeatReceived = Instant.now();
					}
					
				}
				else if (mainCmd.compareTo("SOCKET_TIMEOUT") == 0 || mainCmd.compareTo("NACK") == 0 || mainCmd.compareTo("REJECT") == 0 ){
					continue;
				} 
				else {
					debug(mainCmd + " COMMAND NOT FOUND, IGNORING ",level1);
				}
			}
			// if (nodeId == 0) {
			// 	prepare();
			// }
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
