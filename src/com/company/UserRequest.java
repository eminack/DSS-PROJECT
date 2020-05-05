package com.company;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

class UserRequest extends Thread {
    final Scanner scn;
    final DataOutputStream dos;
    final DataInputStream dis;

    // Constructor
    public UserRequest(Scanner scn, DataOutputStream dos, DataInputStream dis) {
        this.scn = scn;
        this.dos = dos;
        this.dis = dis;
    }

    @Override
    public void run() {
        while (true) {
            String hh = scn.nextLine();
            try {
                dos.writeUTF(hh);
            } catch (Exception e) {
                ;
            }
        }
    }

}