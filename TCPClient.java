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
                Message msg = (Message)ois.readObject();
                System.out.println("Client received " + msg.toString());

                // Extract client information
                InetAddress senderIp = receivePacket.getAddress();
                int senderUdpPort = receivePacket.getPort();

                // Client received a file request
                if(msg.getType().equals(Type.FILE_REQ)) { // ADD CHECK TO SEE IF CLIENT OWNS THE FILE BEFORE SENDING FILE_CONF

                    int tcpPort = acquireTCPPort(); // Get a random TCP port
                    Message fileConf = new Message(Type.FILE_CONF, msg.getRq(), tcpPort); // Send a file conf message to the client who requested the file
                    fileConf.send(senderIp, senderUdpPort, ds);

                    // Wait for other client to be ready to receive the file
                    sleep(2000);

                    // Send file over provided TCP socket
                    Socket tcpSocket = new Socket(InetAddress.getByName(HOST), tcpPort);
                    Thread fileSender = new Thread(new FileSender(tcpSocket, msg.getRq(), msg.getName()));
                    fileSender.start();
                }

                // Client received a file conf
                if(msg.getType().equals(Type.FILE_CONF)) {
                    // Wait for file over provided TCP socket
                    ServerSocket server = new ServerSocket(msg.getSocket(),1, senderIp);
                    Socket client = server.accept();
                    System.out.println("Client waiting for file");
                    BufferedWriter bw = new BufferedWriter(new FileWriter("temp.txt"));
                    Message file = null;
                    while(file == null || !file.getType().equals(Type.FILE_END)){
                        file = Message.receive(client);
                        System.out.println("Client received " + file.toString());
                        // Put file back together
                        bw.write(file.getText());
                        bw.flush();
                    }
                    String filename = file.getName();
                    // RENAME AND ADD TO LIST OF OWNED FILE
                    bw.close();
                    // Close the sockets created to receive the file
                    client.close();
                    server.close();
                }

                // Clear buffer
                buffer = new byte[BUFFER_SIZE];
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static int acquireTCPPort() {
        final int MIN_PORT_NUMBER = 1025;
        final int MAX_PORT_NUMBER = 65535;
        Random r = new Random();
        return r.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
    }

    static class FileSender implements Runnable {
        private static final int MAX_CHAR = 200;
        private static final int EOF = -1;
        private static final int FILE_OFFSET = 0;
        private Socket socket;
        private int rq;
        private String filename;

        public FileSender(Socket socket, int rq, String filename) {
            this.socket = socket;
            this.filename = filename;
            this.rq = rq;
        }

        // Send requested file over provided TCP socket
        @Override
        public void run() {
            TextFile textFile = new TextFile(new File(filename));
            try  {
                // Setup to read textFile character by character
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
                    msg.send(socket); // Send the message over TCP socket
                }
            } catch (IOException e) {
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

