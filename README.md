# Multi Paxos 
CS751 Course Project

## How to run
1. Run javac Paxos.java (we used Java 1.8)
2. Run java Paxos which will start a console
3. Enter the number of nodes (say 20)
4. You can see the log of each node in nodeLog directory
5. The values sent by client are stored in a file "log" created in current directory.
6. In the console you can enter the following commands: 
    - kill \<nodeId\> 
         * Kills the node with Id \<nodeId\>
    - client \<value\>
        * Is a request from client to accept his/her \<value\>