package com.uom.cse.distsearch.util;


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Utility {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utility.class);

    private Utility() {
    }

    public static String sendTcpToBootstrapServer(String message, String ip, int port) throws IOException {

          DatagramSocket sock = new DatagramSocket(port);

        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length,InetAddress.getByName(ip), port);
        sock.send(packet);
        byte[] buffer = new byte[65536];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        sock.receive(response);
        sock.close();

        byte[] data = response.getData();
        String msg = new String(data, 0, response.getLength());
        LOGGER.info("Recieved :" + msg);
        return msg;
    }
}
