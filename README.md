# DSS-PROJECT
A Distributed Peer to Peer File sharing Network
Done as a part of Distributed Systems Course at IIIT-Allahabad
***every client should be connected to same network as the server for the code to work

/* Steps to run the code */
1. create a database named "peers" & Run the Db.sql command to setup the table.
2. Extract the the contents code.zip folder into a new folder.
3. Open the project(new folder) on IntelliJ & set the database URL, Username,Password in server2.java according to your system.
4. If you want chunking set the chunking flag in Server2.java to 1 else set to 0.
5. Run the server2.java file.
6. Similarly run the Client.java file after entering the server IP (in the Client.java file Main) 
  in same pc as well as some other pc which will be the peers.
7. On connecting with server, each client will get list of services they can access
8. Peers can send request using the UserRequest.java class dedicated for user input running on separate thread.
9. On entring option(1) the the client will be promted to enter filename to search. So distributed search will 
   be performed and thus if anyone has the file the user will get to know about it.
10. On entering option(3) the client will be promted to enter filename to download. Then the server will broadcast 
   this request to all peers.
11. The peers will search the file in their sharable folder respectively using a already constructed trie-data 
   structure and start their mini server for file transfer on a separate thread (ClientHandler2.java) and send 
    the port for this mini server to server.
12. Server on receiving the message from each client it will send the IPs and Ports of peers having this file 
   and then the requested client can select a peer and start a connection (ClientHandler3.java) and get the file.
13. On pressing 2 the Client will be removed from the database of server and will also stop.
14. If a client is asking to get the same file transfered to it again & again , then this client will provide 
    this file to any one of the client which is not having it , so that load on a single client is reduced &
    single point of failure is removed. This transfer will happen automatically and user doesn't need to specify
    anything
© 2020 GitHub, Inc.
