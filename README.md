# Multi Paxos 
CS751 Course Project

 
1. If ran freshly It removes the files in "nodeLog" folder in current directory. 
        - If this folder doesn't exists then it creates one. 
2. The values sent by client are stored in a file "log" created in current directory.

3. Enter the total number of Nodes. Only Integer
4. Options available: 
    1. kill \<nodeId\> 
         * Kills the node with Id \<nodeId\>
    2. client \<value\>
        * Is a request from client to accept his/her \<value\>

    