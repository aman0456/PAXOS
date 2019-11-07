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

	Integer acceptCount;

	ArrayList<Integer> promisedIds;
	
	Duration mainTimeoutDuration = Duration.ofSeconds(5);
	Duration secondaryTimeoutDuration = Duration.ofSeconds(1);
	Instant mainStartTime;
	Instant secondaryStartTime;
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
		
		this.acceptCount = 0;
		
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
	Boolean checkTimeout(Instant startTime, Duration timeoutDuration){
		Duration timeElapsed = Duration.between(startTime, Instant.now());
		return timeElapsed.toMillis() > timeoutDuration.toMillis();
	}

	void resetPrepare() throws IOException {
		isPromiseWait = true;
		isAcceptWait = false;
		isAcceptMajority = false;
		isPromiseMajority = false;
		acceptCount = 0;
		promisedIds.clear();
		mainStartTime = Instant.now();
	}

	void prePrepare() throws IOException {
		/* To send Pre-Prepare Msg */
		resetPrepare();
		mainStartTime = Instant.now();

		debug("Sending Prepare " + curPropPid);
		String prepareMsg = "PREPARE:"+nodeId+":"+curPropPid;
		comm.sendAll(prepareMsg, nodeId);

	}
	void prepare() throws IOException {
		/* To send Prepare Msg */
		resetPrepare();
		mainStartTime = Instant.now();

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
		mainStartTime = Instant.now();

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
	
	void handlePrepareTimeout(String msg) throws IOException {
		curPropPid++;
		debug(msg);
		prepare();
	}


	void handleAcceptTimeout(String msg) throws IOException {
		// debug("Timeout while waiting for acceptances " + curPropPid + " "+ valueSend);
		curPropPid++;
		debug(msg);
		prepare();
	}

	@Override
	public void run() { 
		debug("Inside Node run " + nodeId);
		String valueSend = "";
		int acceptCount = 0;
		String curMax = null;
		try {
			while(true){
				String cmd = comm.receive(nodeId);
				String[] parList = cmd.trim().split("\\s*:\\s*");
				String mainCmd = parList[0];
				//checking for promise timeout
				if (isPromiseWait){
					if(checkTimeout(mainStartTime, mainTimeoutDuration) ||
						(isPromiseMajority && checkTimeout(secondaryStartTime, secondaryTimeoutDuration))) {
						isPromiseWait = false;

						if (isPromiseMajority) {
							debug("Reached Majority of " + promisedIds.size() + " Promises for proposal " + curPropPid);
							assert !isAcceptMajority;
							assert acceptCount == 0;
							if (curMax != null) valueSend = curMax;
							for (Integer tosendId : promisedIds){
								accept_request(tosendId, valueSend);
							}
						}
						else {
							debug("Prepare Timeout for proposal " + curPropPid);
							handlePrepareTimeout("Prepare Timeout : Pid " + curPropPid + " curVal :"+ curMax);
						}
					}
				}
				
				//checking for accept timeout
				if (isAcceptWait){
					assert !isAcceptMajority;
					if(checkTimeout(mainStartTime,mainTimeoutDuration)) handleAcceptTimeout("Accept Timeout  Pid :" + curPropPid + " curVal :"+ curMax);
				}
				
				if (mainCmd.compareTo("SOCKET_TIMEOUT") == 0){
					// debug();
				}

				else if (mainCmd.compareTo("CMDPREPARE") == 0){
					// If another request comes in middle of protocol, it rejected 
					// TODO;
					valueSend = parList[1];
					prepare();
				}

				else if (mainCmd.compareTo("PREPARE") == 0){
					Integer fromId = Integer.parseInt(parList[1]);
					Integer propPid = Integer.parseInt(parList[2]);
					debug("Received Prepare " + propPid + " from " + fromId);
					promise(propPid, fromId);
				}

				else if (mainCmd.compareTo("PROMISE") == 0){

					Integer fromId = Integer.parseInt(parList[1]);
					Integer propPid = Integer.parseInt(parList[2]);
					Boolean hasReceiverAccepted = parList.length > 3;
					debug("Received Promise " + propPid + " " + curPropPid + " from " + fromId);
					if (propPid != curPropPid) {
						debug("Ignoring promise for different proposal");
						continue;
					}
					promisedIds.add(fromId);
					
					if (hasReceiverAccepted) {
						Integer propIdAccepted = Integer.parseInt(parList[3]);
						String valueAccepted = parList[4];
						if (curMax == null) curMax = valueAccepted;
						else {
							assert ("a".compareTo("b") < 0);
							curMax = (curMax.compareTo(valueAccepted) > 0 ? curMax : valueAccepted);
						}
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
					String valueAcc = parList[3];
					accept(acceptPid, fromId, valueAcc);
				}

				else if (mainCmd.compareTo("ACCEPT") == 0){
					acceptCount++;
					Integer fromId = Integer.parseInt(parList[1]);
					Integer acceptPid = Integer.parseInt(parList[2]);
					String valueAcc = parList[3]; 
					debug("Got Acceptance " + curPropPid + " Value: " + valueAcc + " from " + fromId );
					
					if (acceptPid != curPropPid){
						debug("Ignoring acceptance for different accept request");
						continue;
					}

					if (!isAcceptMajority && comm.checkMajority(acceptCount)){
						debug("Reached Majority " + acceptCount + " Acceptances " + curPropPid + " Value: " + valueAcc);
						isAcceptMajority = true;
						isAcceptWait = false;
						//TODO: SEND TO ALL
					}
				} 
				else if (mainCmd.compareTo("SOCKET_TIMEOUT") == 0 || mainCmd.compareTo("NACK") == 0 || mainCmd.compareTo("REJECT") == 0 ){
					continue;
				} 
				else {
					debug(mainCmd + " COMMAND NOT FOUND, IGNORING ");
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
