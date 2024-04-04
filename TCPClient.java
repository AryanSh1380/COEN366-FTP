import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class TCPClient {
    private static final String HOST = "127.0.0.1";
    private static int UDP_PORT;
    private static final int BUFFER_SIZE = 65535;

    public static void main(String[] args) {

        try {
            // Request the client UDP port
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter UDP port for the client");
            String port = sc.nextLine();
            UDP_PORT = Integer.valueOf(port);

            // Listen to incoming messages on selected UDP port
            DatagramSocket ds = new DatagramSocket(UDP_PORT);
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            System.out.println("Client listening to port " + UDP_PORT);

            // Start thread to send file requests to other clients
            Thread senderThread = new Thread(new RequestSender(InetAddress.getByName(HOST), ds));
            senderThread.start();
            System.out.println("Enter the UDP socket of the client followed by the file name to send a FILE_REQ");

            while(true){
                // Receive packet
                ds.receive(receivePacket);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
                Message messageReceived = (Message)ois.readObject();
                System.out.println("\nClient received " + messageReceived.toString());

                // Extract sender information
                InetAddress senderIp = receivePacket.getAddress();
                int senderUdpPort = receivePacket.getPort();

                // Client received a file request
                if(messageReceived.getType().equals(Type.FILE_REQ)) { // ADD CHECK TO SEE IF CLIENT OWNS THE FILE BEFORE SENDING FILE_CONF

                    int tcpSocketNumber = acquireTCPSocketNumber(); // Get a random TCP socket number
                    Message fileConfMessage = new Message(Type.FILE_CONF, messageReceived.getRq(), tcpSocketNumber); // Send a file conf message to the client who requested the file
                    fileConfMessage.send(senderIp, senderUdpPort, ds);

                    // Send file over provided TCP socket
                    Thread fileSender = new Thread(new FileSender(senderIp, tcpSocketNumber, messageReceived.getRq(), messageReceived.getName()));
                    fileSender.start();
                }

                // Client received a file conf
                if(messageReceived.getType().equals(Type.FILE_CONF)) {
                    Thread fileReceiver = new Thread(new FileReceiver(messageReceived.getSocketNumber()));
                    fileReceiver.start();
                }

                // Clear buffer
                buffer = new byte[BUFFER_SIZE];
            }

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    public static int acquireTCPSocketNumber() {
        final int MIN_PORT_NUMBER = 1025;
        final int MAX_PORT_NUMBER = 65535;
        Random r = new Random();
        return r.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
    }

    static class FileReceiver implements Runnable {
        private int socketNumber;

        FileReceiver(int socketNumber) {
            this.socketNumber = socketNumber;
        }

        // Wait for file over provided TCP socket number
        @Override
        public void run() {
            try {
                Socket tcpSocket = new Socket(InetAddress.getByName(HOST), socketNumber);
                System.out.println("Client waiting for file");
                BufferedWriter bw = new BufferedWriter(new FileWriter("temp.txt"));
                Message file = null;
                while (file == null || !file.getType().equals(Type.FILE_END)) {
                    // ADD TIMEOUT IF CLIENT DOES NOT RECEIVE ANYTHING
                    file = Message.receive(tcpSocket);
                    System.out.println("Client received " + file.toString());
                    // Put file back together
                    bw.write(file.getText());
                    bw.flush();
                }
                String filename = file.getName();
                // RENAME AND ADD TO LIST OF OWNED FILE
                bw.close();
                tcpSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class FileSender implements Runnable {
        private static final int CONNECTION_DELAY = 2000;
        private static final int MAX_CHAR = 200;
        private static final int EOF = -1;
        private static final int FILE_OFFSET = 0;
        private static final int BACKLOG_SIZE = 1;
        private InetAddress destinationIp;
        private int socketNumber;
        private int rq;
        private String filename;

        public FileSender(InetAddress destinationIp, int socketNumber, int rq, String filename) {
            this.destinationIp = destinationIp;
            this.socketNumber = socketNumber;
            this.filename = filename;
            this.rq = rq;
        }

        // Send requested file over provided TCP socket
        @Override
        public void run() {
            try  {
                // Accept TCP connection
                ServerSocket server = new ServerSocket(socketNumber, BACKLOG_SIZE, destinationIp);
                server.setSoTimeout(CONNECTION_DELAY);
                Socket destination = server.accept();
                // Setup to read textFile character by character
                TextFile textFile = new TextFile(new File(filename));
                FileReader fr = new FileReader(textFile.getFile());
                BufferedReader br = new BufferedReader(fr);
                char[] buffer = new char[MAX_CHAR];
                int ch;
                int chunk = 1;
                Message msg = new Message(Type.FILE, rq, filename);
                // Read the textFile character by character
                while ((ch = br.read(buffer, FILE_OFFSET, buffer.length)) != EOF) {
                    String text = new String(buffer, FILE_OFFSET, ch); // Get the text
                    msg.setText(text);
                    msg.setChunk(chunk); // Update the message attributes
                    chunk++;
                    if(text.toCharArray().length < MAX_CHAR) { // Special END_FILE message for last chunk
                        msg.setType(Type.FILE_END);
                    }
                    msg.send(destination); // Send the message over TCP socket
                }
                // Close the sockets created to receive the file
                destination.close();
                server.close();
            } catch(SocketTimeoutException e){
                System.err.println("Timeout waiting for client connection");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class RequestSender implements Runnable {
        InetAddress ip;
        private DatagramSocket socket;
        private static int rq = 1;
        RequestSender(InetAddress ip, DatagramSocket socket){
            this.socket = socket;
            this.ip = ip;
        }

        // Send file requests to another client
        @Override
        public void run() {
            try {
            // Communication setup
            Scanner sc = new Scanner(System.in);
            System.out.println("Client ready to send file requests");

            // Wait for user to provide the client port and the file to request
            while (true) {
                String clientPort = sc.nextLine();
                String filename = sc.nextLine();

                // Send FILE_REQ message
                Message msg = new Message(Type.FILE_REQ, rq, filename);
                msg.send(ip, Integer.valueOf(clientPort), socket);
                getNextRq();
            }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        public int getNextRq() {
            return rq++;
        }
    }
}

