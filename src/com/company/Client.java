package com.company;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.lang.ProcessBuilder;

public class Client {

    static final int fnamesize = 255;

    /*Trienode is a class which save already present filenames into a trie for fast searching of File*/

    static class Trienode{
         
        Trienode[] childs = new Trienode[fnamesize];
        boolean endofword;

        public Trienode() {
            endofword = false;
            for(int i=0;i<fnamesize;i++){
                childs[i]=null;
            }
        }  
    }
    static Trienode root;

    /*inserts The Filename into the Trie*/

    static void insert(String key)
    { 
        int level; 
        int length = key.length(); 
        int index; 
        Trienode pCrawl = root; 
       
        for (level = 0; level < length; level++) 
        { 
            index = key.charAt(level) - '\0'; 
            if (pCrawl.childs[index] == null) 
                pCrawl.childs[index] = new Trienode(); 
       
            pCrawl = pCrawl.childs[index]; 
        } 
       
        // mark last node as leaf 
        pCrawl.endofword = true; 
    }

    /*searches the key in Trie and returns True if Found , False otherwise */
    static boolean search(String key) 
    { 
        int level; 
        int length = key.length(); 
        int index; 
        Trienode pCrawl = root; 
       
        for (level = 0; level < length; level++) 
        { 
            index = key.charAt(level) - '\0'; 
       
            if (pCrawl.childs[index] == null) 
                return false; 
       
            pCrawl = pCrawl.childs[index]; 
        } 
       
        return (pCrawl != null && pCrawl.endofword); 
    } 


    public static void main(String[] args) throws IOException {
       InetAddress myip_is = InetAddress.getLocalHost();
       String myipfordisplay = myip_is.getHostAddress();
        int port = 5056;
        int flag = 0;
        Thread tt2 = null;
        int flag4 = 0;
        String myIP = null;
        String ip="192.168.2.8";
        int port1=5056;
        
        while(true)
        {
            flag=0;
            
            /*onlineClients contains the list of all clients currently connected to Server*/
            
            ArrayList<String> onlineClients = new ArrayList<>();
            try 
            {
                Scanner scn = new Scanner(System.in);
                Socket s = new Socket(ip, port1);
                System.out.println("My IP " + myipfordisplay);
                
                /*construction of TrieNode*/
                
                root=new Trienode();
                final File folder = new File("src/peertopeer");
                int c=0;
                for (final File f : folder.listFiles()) 
                {
                    insert(f.toString().substring(15));
                }

                /*obtaining input and out streams*/
                
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                
                /* the following loop performs the exchange of
                 information between client and client handler*/
                
                while (true) {
                    String received = dis.readUTF();
                    received = received.trim();
                    System.out.println(received);
                    
                    /*when pressed 2 on client then server sends exit to client and it shuts down and Sends Exit to the Handler*/

                    if (received.compareTo("Exit")==0) {
                        dos.writeUTF("Exit");
                        System.exit(0);
                    }
                   
                    /* The server,Handler provides the list of online clients to each Client*/
                    
                    if (received.split(" ")[0].equals("IP"))
                    {
                        myIP = received.split(" ")[1];
                        for(int i=2;i<received.split(" ").length;i++)
                        {
                            onlineClients.add(received.split(" ")[i]);
                        }
                    }
                    
                    /* Client writes its own MAC address to its outputstream*/
                    
                    else if (received.equals("MAC")) {
                        String macadd = getmac();
                        dos.writeUTF(macadd);
                    }
                    else if (received.split(" ")[0].equals("OnlineClients")) {
                        System.out.println(received);
                    }

                    /*In this case , the client receive the request to search a filename 
                    in its storage so it searhes and respondes to its corresponding handler */
                    
                    else if(received.split(" ")[0].equals("HaveFile?")){
                        if(search(received.split(" ")[1]))
                        {
                            dos.writeUTF("HaveFile Yes " + received.split(" ")[2]);
                        }
                        else
                        {
                            dos.writeUTF("HaveFile No " + received.split(" ")[2]);
                        }
                    }

                    /*In This case, the client receives that the server-client handler 
                    of this client has send requestfor file search to *temp* no of other clients*/ 
                    
                    else if(received.split(" ")[0].equals("ForSearching"))
                    {

                        //temp = no of clients to which search request has been sent
                        
                        int temp=Integer.parseInt(received.split(" ")[1]);
                        int fla=0;

                        /*In this loop the client waits for other clients responses 
                           to the search performed in each of them */ 
                        
                        for(int i=0;i<temp;i++)
                        {
                            String rec = dis.readUTF();
                            if(rec.equals("Yes") && fla==0){
                                System.out.println("Yes File Exists in one of the clients");
                                fla=1;   
                            }
                        }
                        if(fla==0)
                        {
                            System.out.println("The requested file does not exist");
                        }
                        dos.writeUTF("UNLOCKME");
                    }

                    /* In this case, the handler is asking this Client if it have a particular file ?
                        If Yes then initiate the transfer to the client which requested the file */
                    
                    else if (received.split(" ")[0].equals("DoYouHaveFile")) {
                        
                        //receive file name
                        String filename = received.split(" ")[1];
                        System.out.println("Starting mini server Requested filename - " + filename);
                        
                        //receive chunking flag
                        int chunking = Integer.parseInt(received.split(" ")[2]);
                        
                        DataInputStream minidis = new DataInputStream(s.getInputStream());
                        DataOutputStream minidos = new DataOutputStream(s.getOutputStream());

                        final File folders = new File("./src/peertopeer/");
                        int flag3=0;

                        /* Search the file in /src/peertopeer folder using already constructed Trie*/
                        
                        if(search(filename))
                        {
                            flag3=1;
                            filename="./src/peertopeer/"+filename;
                        }
                        
                        /* flag3 = 1 : if the file was found,  else flag3 : 0*/
                        
                        if(flag3==1)
                        {
                            /*if Chunking flag was disabled*/
                            if(chunking==0)
                            {
                                port++;
                                Thread t1 = new ClientHandler2(port,filename,0);
                                t1.start();
                            }

                            /*if chunking Flag was enabled*/
                            else
                            {
                                port++;
                                Thread t1 = new ClientHandler2(port,filename,1);
                                t1.start();
                                port++;
                                Thread t2 = new ClientHandler2(port,filename,2);
                                t2.start();
                            }
                        }

                        /* received : contains the IP of the client which requested the file */
                        received = dis.readUTF();

                        if (flag3 == 1)
                        {
                            if(chunking==0)
                                dos.writeUTF("ForPeer " + received + " " + Integer.toString(port));
                            else
                                dos.writeUTF("ForPeer " + received + " " + Integer.toString(port-1));
                        }    
                        else
                            dos.writeUTF("ForPeer " + received + " " + "No");
                    }

                    /* CASE when the handler replies with chunking flag & filename asking this Client to 
                        inform others to search the file and connect to one having it*/ 
                   
                    else if (received.split(" ")[0].equals("Connect")) 
                    {
                        tt2.suspend();      //stop reading input from the USER temporarily 
                        
                        /*Get the value of chunking flag*/

                        int chunking = Integer.parseInt(received.split(" ")[1]); 
                        
                        //Get the filename to be searched
                        String filename = dis.readUTF();

                        //get the replication status
                        String replication = dis.readUTF();

                        //received2 = no of currently online clients 
                        String received2 = dis.readUTF();

                        //list of IP addresses of Client which do not have the file
                        ArrayList<String> returnIPsNoFile = new ArrayList<>();
                        
                        //list of ip addreses & ports of clients which have the file
                        ArrayList<String> returnIPs = new ArrayList<>();
                        ArrayList<String> returnPORTs = new ArrayList<>();
                        
                        for (int i = 0; i < Integer.parseInt(received2); i++) {
                            received = dis.readUTF();
                            if (!received.split(" ")[0].equals("No")) {
                                returnIPs.add(received.split(" ")[2]);
                                returnPORTs.add(received.split(" ")[3]);
                            }
                            else
                            {
                                returnIPsNoFile.add(received.split(" ")[3]);
                            }
                        }

                        /* Initiate the transfer */

                        System.out.println("@@@@@@@@@@@@@@@@@@ ");
                        
                        /* IF any Client has the file and chunking flag = 0, then this if block executes 
                            and this Client receives the filefrom that other Client(returnIP[0]) which has the file*/
                        
                        if ((returnIPs.size() >= 1 && chunking == 0)) 
                        {
                            System.out.println("Connecting to " + returnIPs.get(0) + "on port No : " + returnPORTs.get(0));
                            
                            /* a new thread is spawned which connects to the Client having the file through socket and receives the fwhole ile*/
                            Thread t3 = new ClientHandler3(returnIPs.get(0), Integer.parseInt(returnPORTs.get(0)),
                                    filename, replication, returnIPsNoFile,dos,chunking);
                            t3.start();
                            System.out.println("Peer to peer connected");
                        } 

                        /*case when Chunking flag = 1, then this block gets executed*/
                        else if((returnIPs.size() >= 1 && chunking == 1)) 
                        {   
                            /*if only 1 Client has the File and the Client requesting the File has requested the same file > 4 times
                                then 3 things happen:

                                1. The File is transferred to the Client by spawning a new thread which connects to the client having the file
                                    and receives that file in 2 fragments because the chunking was enabled.
                                2. The 2 fragmenst are merged sequentially.
                                3. This Client tells the Client-handler the ip of one of Clients not having that File. So that the handler can 
                                    initiate the transfer of that File to that Clients(not having the file) because the demand of this particular File is quite high.

                             if not the above case then only (1) & (2) happens */


                            if(returnIPs.size() == 1)
                            {

                                System.out.println("Connecting to " + returnIPs.get(0) + "on port No : " + returnPORTs.get(0) + " for PART-1");
                                Thread t3 = new ClientHandler3(returnIPs.get(0), Integer.parseInt(returnPORTs.get(0)),
                                        filename+"1", "Replicate No", returnIPsNoFile,dos,chunking);

                                System.out.println("Connecting to " + returnIPs.get(0) + "on port No : " + String.valueOf(Integer.parseInt(returnPORTs.get(0))+1) + " for PART-2");
                                Thread t4 = new ClientHandler3(returnIPs.get(0), Integer.parseInt(returnPORTs.get(0))+1,
                                        filename+"2", replication, returnIPsNoFile,dos,chunking);
                                t3.start();
                                t4.start();
                                System.out.println("Peer to peer connected");
                                
                                t3.join();
                                t4.join();

                                //merging the File using FileMerger Class
                                FileMerger fm = new FileMerger(System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"1",System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"2",System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename);
                                fm.merge();
                                insert(filename);
                                File file1 = new File(System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"1"); 
                                File file2 = new File(System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"2"); 

                                file1.delete();
                                file2.delete();

                                /* Part where it tells the Handler to replicate the File if the condition mentioned earlier satisfies*/
                               
                                if(replication.equals("Replicate Yes"))
                                {
                                    if(returnIPsNoFile.size()>0)
                                    {
                                        dos.writeUTF("Replicate File");
                                        dos.writeUTF(returnIPsNoFile.get(0));
                                        dos.writeUTF(filename);
                                    }
                                }

                                System.out.println("Files Merged Successfully");
                            }

                            /*if more than 1 Client has the File and the Client requesting the File has requested the same file > 4 times
                                then 4 things happen:

                                1. The File is transferred to the Client by spawning a new thread which connects to the client-1 having the file
                                    and receives the first fragment (First Half) of the file because the chunking was enabled.
                                2. Rest of the second part of the File is transferred to the Client from Client-2 having the File
                                3. The two received Files are merged sequentially
                                4. This Client tells the CLienthandler the 1 of Clients not having that File. So that the handler can 
                                    initiate the transfer of that File to that Clients(not having the file) because the demand of this particular File is quite high.

                             if not the above case then only (1) & (2) & (3) happens */
                            
                            else
                            {
                                System.out.println("Connecting to " + returnIPs.get(0) + "on port No : " + returnPORTs.get(0) + " for PART-1");
                                System.out.println("Connecting to " + returnIPs.get(1) + "on port No : " + returnPORTs.get(1) + " for PART-2");
                                
                                //connecting to 1st Client for 1st part of file
                                Thread t3 = new ClientHandler3(returnIPs.get(0), Integer.parseInt(returnPORTs.get(0)),
                                        filename+"1", "Replicate No", returnIPsNoFile,dos,chunking);

                                //connecting to 2nd Client for the 2nd part of File
                                Thread t4 = new ClientHandler3(returnIPs.get(1), Integer.parseInt(returnPORTs.get(1))+1,
                                        filename+"2", replication, returnIPsNoFile,dos,chunking);
                                t3.start();
                                t4.start();
                                System.out.println("Peer to peer connected");

                                t3.join();
                                t4.join();

                                //mergingthe File using FileMerger Class
                                FileMerger fm = new FileMerger(System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"1",System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"2",System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename);
                                fm.merge();
                                insert(filename);
                                File file1 = new File(System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"1"); 
                                File file2 = new File(System.getProperty("user.dir").replace('\\', '/') + "/src/peertopeer/"+filename+"2"); 

                                file1.delete();
                                file2.delete();

                                /* Part where it tells the Handler to replicate the File if the condition mentioned earlier satisfies*/

                                if(replication.equals("Replicate Yes"))
                                {
                                    if(returnIPsNoFile.size()>0)
                                    {
                                        dos.writeUTF("Replicate File");
                                        dos.writeUTF(returnIPsNoFile.get(0));
                                        dos.writeUTF(filename);
                                    }
                                }
                                System.out.println("Files Merged Successfully");
                            }
                            
                        } 
                        else 
                        {
                            System.out.println("No peer has this file!");
                        }
                        /*Client Writes UNLOCKME to indicate the client handler that it 
                            is not stuck doing any work and is ready for listening to client-handler requests*/
                        dos.writeUTF("UNLOCKME");

                        //resume reading of user input from User thorugh this tt2 thread
                        tt2.resume();
                    }
                    else
                    {
                        /* Prints the message :Press(1) for searching.... to the user OR
                            Prints the message : Enter the name of file to search.... OR
                            Prints the message : Enter the Name of file you want*/

                        System.out.println(received);

                        if (flag == 0)
                        {
                            /* UserRequest class reads what the user inputs after the above messsage was printed*/
                            tt2 = new UserRequest(scn, dos, dis);
                            tt2.start();
                            flag = 1;
                            int a = 12;
                            if (a == 13)
                                break;
                        }
                    }
                }

                // closing resources
                scn.close();
                dis.close();
                dos.close();
            
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("#############");
                tt2.stop();
                String ip2 = ip;
                for (int i=0;i<onlineClients.size();i++)
                {
                    if(!ip2.equals(onlineClients.get(i).trim()))
                    {
                        ip = onlineClients.get(i);
                        break;
                    }
                }

                /* Check if Server is  shut down because myIP always contains the IP of the server ,
                    if yes it kills the client process*/
                if(ip.equals(myIP))
                {
                    System.exit(0);
                }
                else
                {
                    System.exit(0);
                }
            }
        }
    }

    /* This function searches the file name "pattern" inside
     Folder "folder" and stores the file path into ArrayList result */
    public static void search(final String pattern, final File folder, List<String> result) {  
        for (final File f : folder.listFiles()) {
            if (f.isFile()) {
                if (f.getName().matches(pattern)) {
                    result.add(f.getAbsolutePath());
                }
            }

        }
    }

    /* This Funtion returns the MAC Address */
    public static String getmac() {
        InetAddress ip;
        String macadd = "";
        try {

            ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            macadd = sb.toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return macadd;
    }

}
