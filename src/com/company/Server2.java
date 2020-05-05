
package com.company;
import java.awt.event.MouseListener;
import java.io.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


public class Server2 {
    public static void main(String[] args) throws IOException {
        
        /*server is listening on port 5056 */
        
        System.out.println("Hello");
        ServerSocket ss = new ServerSocket(5056);
        ArrayList<String> onlineClients = new ArrayList<>();
        HashMap<String, DataOutputStream> outputstream = new HashMap<>();
        HashMap<String, DataInputStream> inputstream = new HashMap<>();
        HashMap<String, Integer> locking = new HashMap<>();
        HashMap<String, Integer> peerLocking = new HashMap<>();
        HashMap<String, Integer> FileCount = new HashMap<>();
        int chunking = 0;
        
        /* running infinite loop for getting client requests and 
        assigning a new thread for each new client connected */
        
        while (true) {
            Socket s = null;
            try {

                /* socket object to receive incoming client requests*/
                
                s = ss.accept();
                System.out.println("A new client is connected : " + s);
                System.out.println("Ip Address  =  " + s.getInetAddress());

                String temp = s.getInetAddress().toString();
                String ip = temp.substring(1);
                int i = 0;
                for (i = 0; i < onlineClients.size(); i++) {
                    if (onlineClients.get(i).equals(ip))
                        break;
                }

                /*obtaining input and output streams*/
                
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                if (i == onlineClients.size()) {
                    onlineClients.add(ip);
                    outputstream.put(ip, dos);
                    inputstream.put(ip, dis);
                    locking.put(ip, 0);
                    peerLocking.put(ip,0);
                }
                System.out.println("Assigning new thread for this client");

                /*create a new thread object for handling each client*/
                
                Thread t = new ServerClientHandler2(s, dis, dos, ip, onlineClients, inputstream, outputstream, locking, FileCount, peerLocking, chunking);
                String allpeers = ip;
                for (i = 0; i < onlineClients.size(); i++) {
                    allpeers += " "+onlineClients.get(i);
                }
                System.out.println("All peers : "  + allpeers);
                
                /*Tell all the clients about the currently online Clients*/
                for (i = 0; i < onlineClients.size(); i++) {
                       outputstream.get(onlineClients.get(i)).writeUTF("IP "+allpeers);
                }
                
                /*Invoking the start() method*/
                
                t.start();

            } catch (Exception e) {
                s.close();
                e.printStackTrace();
            }
        }
    }
}




class ServerClientHandler2 extends Thread {

    final DataInputStream dis;
    final DataOutputStream dos;
    final Socket s;
    String ip_address;
    String mac_address;
    int flag = 0;
    final String JDBC_Driver_Class = "com.mysql.cj.jdbc.Driver";
    final String DB_URL = "jdbc:mysql://localhost/peers?autoReconnect=true&useSSL=false";
    final String USER = "root";
    final String PASS = "oracle";
    ArrayList<String> onlineClients;
    HashMap<String, DataInputStream> inputstream;
    HashMap<String, DataOutputStream> outputstream;
    HashMap<String, Integer> locking;
    HashMap<String, Integer> FileCount;
    HashMap<String, Integer> peerLocking;
    int chunking;
 
    public ServerClientHandler2(Socket s, DataInputStream dis, DataOutputStream dos, String ip_address,
            ArrayList<String> onlineClients, HashMap<String, DataInputStream> inputstream,
            HashMap<String, DataOutputStream> outputstream, HashMap<String, Integer> locking,
            HashMap<String, Integer> FileCount, HashMap<String, Integer> peerLocking, int chunking) 
    {
        this.s = s;
        this.dis = dis;
        this.dos = dos;
        this.ip_address = ip_address;
        this.onlineClients = onlineClients;
        this.inputstream = inputstream;
        this.outputstream = outputstream;
        this.locking = locking;
        this.FileCount = FileCount;
        this.peerLocking = peerLocking;
        this.chunking = chunking;
    }

    @Override
    public void run() {
        String received;
        String toreturn;
        while (true) {
            try {
                if (flag == 0) {
                	/*client handler writes to client for MAC*/
                   
                    dos.writeUTF("MAC");						

                    /*client reads the MAC from input stream from client to this handlerr*/
                    
                    received = dis.readUTF();					
                    this.mac_address = received;
                    System.out.println("Mac of client : " + received);
                    flag = 1;
                    int sig = 0;
                    try {											
                        
                        /*this block tries to check if the mac address is already present by checking it in DATABASE*/
                        
                        System.out.println("Database connect");
                        Class.forName(JDBC_Driver_Class);
                        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                        Statement stmt = conn.createStatement();
                        String sql = "SELECT * from peers where macaddress = '" + received + "';";
                        ResultSet rs = stmt.executeQuery(sql);
                        String ip_stored = "";
                        while (rs.next()) 
                        {
                            ip_stored = rs.getString("ip_address");
                        }

                        /*case when client already registered*/
                        
                        if (!ip_stored.equals("")) 
                        {						
                            System.out.println("Client Already registered ");
                            if (ip_stored.equals(ip_address)) 
                            {
                                System.out.println("Same IPAddress");
                                sig = 1;
                            } 
                            else 
                            {
                                System.out.println("Different IP address Need to update");
                                sig = 2;
                            }
                        }
                        stmt.close();
                        
                        /*case when the mac is already registered with server but ip-address has changed , so update IP address in database*/
                        
                        if (sig == 2)
                        {												
                            Statement stmt2 = conn.createStatement();
                            String sql2 = "UPDATE peers SET ipaddress = '" + ip_address + "' where macaddress = '"
                                    + received + "';";
                            stmt2.executeUpdate(sql2);
                            stmt2.close();
                        }

                        conn.close();

                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                    /*if  already registered MAC device , then skip the next part
                      else insert the MAC and IP address of client into the database*/
                    
                    if (sig != 0)							
                        continue;							

                    /*Case when the Client is not at all registered in the database so insert MAC & IP address into the DATABASE*/
                    
                    try {
                        Class.forName(JDBC_Driver_Class);
                        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                        Statement stmt = conn.createStatement();
                        String sql = "INSERT INTO peers VALUES ('" + received + "','" + ip_address + "');";
                        stmt.executeUpdate(sql);
                        stmt.close();
                        conn.close();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                } 
                else 
                {
                    /* receive the answer from client*/
                    
                    if (this.locking.get(this.ip_address) == 0) {
                        dos.writeUTF(
                                "Press: (1) to Enter file to search | (2) to de-register your pc | (3) to download files ");
                    }
                    received = dis.readUTF();
                    
                    /*UNLOCKME IS for unlcoking the client, Also when to print the INSTRUCTION PART ? 
						if locking = 0 , then print the instruction else dont print */

                    if(received.equals("UNLOCKME"))
                    {
                        this.peerLocking.put(this.ip_address, 0);
                    }
                    
                    /*In this case , the Clients are telling handler whether they found file or not */
                    
                    else if (received.split(" ")[0].equals("ForPeer")) 
                    {
                    	/*if this clients doesn't have the file then this condition tells the client(which initiated the file search) 
                    		that My CLient (client of this handler),  (its own ip address) doesn't have the file*/
                        
                        if (received.split(" ")[2].equals("No")) {
                            this.outputstream.get(received.split(" ")[1]).writeUTF("No file found "+this.ip_address);
                        } 
                        
                        /*This else case tells the handler to tell the client(which initiated the file search) to connect to the client
                          of this handler because it has the file and has already made a socket on specific port to listen to this connection
							received.split(" ")[2] : port at which the ClientHandler2 is listening to the socket
                         */
                        
                        else 
                        {
                            this.outputstream.get(received.split(" ")[1]).writeUTF("Connect to " + this.ip_address + " " + received.split(" ")[2]);
                            
                        }
                    }

                    /*In this case , the handler receives the answers from respective clients 
                    	on whether they have the file or not , and this handler responds to the 
                    	Yes/No to the client which requested the file */
                    
                    else if(received.split(" ")[0].equals("HaveFile")){
                        if(received.split(" ")[1].equals("Yes")){
                            this.outputstream.get(received.split(" ")[2]).writeUTF("Yes");
                        }
                        else 
                        {
                            this.outputstream.get(received.split(" ")[2]).writeUTF("No");
                        }
                    }


                    /* Case when the handler wants to replicate the File (because demand of that file is quite high)
                    	to a Client not having the File */
                    else if (received.equals("Replicate File"))
                    {
  		
                        String repIp = this.dis.readUTF();
                        String repFile = this.dis.readUTF();
                        this.outputstream.get(repIp).writeUTF("Connect 0");
                        this.outputstream.get(repIp).writeUTF(repFile);
                        this.outputstream.get(repIp).writeUTF("Replicate No");
                        this.outputstream.get(repIp).writeUTF(Integer.toString(1));
                        this.dos.writeUTF("DoYouHaveFile " + repFile + " 0");
                        this.dos.writeUTF(repIp);
                    }

                    /*Client presses 1 on the Menu*/
                    
                    else if(received.equals("1")){
                        this.peerLocking.put(this.ip_address, 1);
                        dos.writeUTF("Enter the Name of the file you want ");
                        String filename = dis.readUTF();
                        System.out.println(filename);
                        int count=0;
                        
                        /*	This loop counts the number of Peers which are not 
                        	Locked (or are not doing any work / free to listen for request) 
                        */
                        
                        for(int i=0;i<this.onlineClients.size();i++)
                        {
                            if(this.onlineClients.get(i)!=this.ip_address && this.peerLocking.get(this.onlineClients.get(i))==0)
                            {
                                count++;
                            }
                        }
                        
                        /*This ForSearching is only written to Client which requested the file from other peers*/
                        
                        dos.writeUTF("ForSearching " + Integer.toString(count));

                        /*Now this loop asks each of the client (excluding the client which asked for file) whether they have the file with them*/
                        
                        for(int i=0;i<this.onlineClients.size();i++)
                        {
                            if(this.onlineClients.get(i)!=this.ip_address && this.peerLocking.get(this.onlineClients.get(i))==0)
                            {
                                this.outputstream.get(this.onlineClients.get(i)).writeUTF("HaveFile? " + filename + " "  + this.ip_address);
                            }
                        }
                        this.locking.put(this.ip_address, 1);
                    }

                    /*Client presses 3 on the Menu */

                    else if (received.equals("3"))
                    {
                        this.peerLocking.put(this.ip_address, 1);
                        dos.writeUTF("Enter the Name of the file you want ");
                        String filename = dis.readUTF();
                        System.out.println(filename);

                        dos.writeUTF("Connect "+Integer.toString(this.chunking));
                        dos.writeUTF(filename);
                        
                        /*This IF-ELSE writes the replication status to the Client*/
                        
                        if(this.FileCount.containsKey(filename))
                        {
                            this.FileCount.put(filename,this.FileCount.get(filename)+1);
                            if(this.FileCount.get(filename)>4)
                            { 
                            	dos.writeUTF("Replicate Yes"); 
                                this.FileCount.put(filename,0); 
                            }
                            else
                                dos.writeUTF("Replicate No");
                        } 
                        else 
                        {
                            this.FileCount.put(filename,1);
                            dos.writeUTF("Replicate No");
                        }

                        String tt = "OnlineClients :";
                        String selectclient = "";
                        dos.writeUTF(Integer.toString(this.onlineClients.size() - 1));

                        for (int i = 0; i < this.onlineClients.size(); i++) 
                        {
                            tt += " " + this.onlineClients.get(i);

                            /* This Loop asks each  of the client (Excluding the one which pressed 3) 
                               whether they have the requested File or not */

                            if (this.onlineClients.get(i) != this.ip_address)
                            {
                                this.outputstream.get(this.onlineClients.get(i)).writeUTF("DoYouHaveFile " + filename + " " + Integer.toString(this.chunking));
                                this.outputstream.get(this.onlineClients.get(i)).writeUTF(this.ip_address);
                            }
                        }
                        this.locking.put(this.ip_address, 1);
                    } 
                    
                    /* If client presses 2 in the Menu then the handler removes the client from the 
                    	database and Writes Exit to output stream to the client*/
                    
                    else if (received.equals("2")) {
                        try {
                            Class.forName(JDBC_Driver_Class);
                            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                            Statement stmt = conn.createStatement();
                            String sql = "DELETE from peers where macaddress = '" + this.mac_address + "';";
                            stmt.executeUpdate(sql);
                            stmt.close();
                            conn.close();
                            System.out.println("Client deleted from Database");
                            dos.writeUTF("Exit");
                            break;
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    else
                    {
                        System.out.println(received);
                        dos.writeUTF("Invalid input");
                    }
                }
            } catch (IOException e) {
                break;
            }

        }

        try {
            
            /* closing resources and removing the exited client from online List*/
            
            System.out.println("Client sends exit...");
            System.out.println("Closing " + this.s.getInetAddress()  + " connection.");
            this.dis.close();
            this.dos.close();
            this.s.close();
            for (int i = 0; i < this.onlineClients.size(); i++) {
                if (this.onlineClients.get(i).equals(this.ip_address)) {
                    System.out.println("this client removed from list");
                    this.onlineClients.remove(i);
                }
            }
            String allpeers = this.ip_address;
            for (int i = 0; i < onlineClients.size(); i++) {
                allpeers += " "+onlineClients.get(i);
            }

            /*update all the clients about the updated online clients List*/
            
            for (int i = 0; i < onlineClients.size(); i++) {
                outputstream.get(onlineClients.get(i)).writeUTF("IP "+allpeers);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
