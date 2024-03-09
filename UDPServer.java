import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class UDPServer {

    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(2308);
            byte[] buffer = new byte[1024];

            // Receive the first message
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            socket.receive(request);

            String message = new String(request.getData(), 0, request.getLength());
            System.out.println("Client: " + message);

            InetAddress clientAddress = request.getAddress();
            int clientPort = request.getPort();
            byte[] sendData = message.getBytes();
            DatagramPacket response = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(response);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
            
        // DatagramSocket ds = new DatagramSocket(3031);
        // while(!ds.isClosed()) {
        //     byte[] buff = new byte[128];
        //     DatagramPacket rcv_hello = new DatagramPacket(buff, buff.length);
        //     ds.receive(rcv_hello);
        //     StringBuilder ret = new StringBuilder();
        //     int i = 0;
        //     byte current = buff[0];
        //     while (current != 0) {
        //         ret.append((char) buff[i]);
        //         i++;
        //         try {
        //             current = buff[i];
        //         } catch(Exception e) {
        //             current = 0;
        //         }
        //     }
        //     String s = ret.toString();
        //     System.out.println(s);
            // if(s.equals("hello")) {
            //     ClientHandler ch = new ClientHandler();
            //     ch.addInput(rcv_hello.getSocketAddress());
            //     ch.addInput(this);
            //     ch.setHandler(new UDPClientHandler());
            //     Thread t = new Thread(ch);
            //     t.start();
            //     clientThreads.add(t);
            // }
    //     }
       
    // static class ClientHandler extends Thread {
    //     private Socket clientSocket;

    //     public ClientHandler() {
    //     }

    //     public void run() {
    //         try {
    //             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    //             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

    //             //receive hello message from client
    //             String HelloMsg = in.readLine();
    //             if ("hello".equals(HelloMsg)) {
    //                 out.println("send_info");

    //                 // Receive Client information
    //                 String clientId = in.readLine();
    //                 String clientDesc = in.readLine();

    //                 // Print Client information 
    //                 System.out.println("Client " + clientId + " registered with description: " + clientDesc);
    //             }
    //             clientSocket.close();
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }
    //     }   
    }
}



