import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;

public class UDPClient {
    private static Message msg;
    private static DatagramSocket socket;
    private static InetAddress clientAddress;
    private static int clientPort;
    private static String clientName;
    private static InetAddress serverAddress;
    private static int serverPort;
    private static InetAddress peerAddress;
    private static int peerPort;
    private static File directory = new File(".");
    private static byte[] buffer;
    private static boolean clientRegistered = false;
    private static final int BUFFER_SIZE = 65535;
    public static void main(String[] args) {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(50000);

            Scanner scanner = new Scanner(System.in);
            List<String> filenamesToPublish = new ArrayList<>();
            List<String> filenamesToRemove = new ArrayList<>();

            System.out.print("Enter Client Name: ");
            clientName = scanner.nextLine();

            System.out.print("Enter client UDP port: ");
            String port = scanner.nextLine();
            clientPort = Integer.valueOf(port);

            System.out.print("Enter Server address: ");
            String serverIP = scanner.nextLine();
            serverAddress = InetAddress.getByName(serverIP);
            
            System.out.print("Enter Server Port Number: ");
            serverPort = scanner.nextInt();

            clientAddress = InetAddress.getByName("localhost"); 
            int reqNumb = 0;
            

            loop: while(!socket.isClosed()) {
                // Displaying menu options
                System.out.println("Select an option:");
                System.out.println("1. REGISTER");
                System.out.println("2. PUBLISH");
                System.out.println("3. DEREGISTER");
                System.out.println("4. REMOVE");
                System.out.println("5. P2P");
                System.out.println("6. EXIT");
                System.out.print("Enter your choice: ");

                // Reading user input
                int choice = scanner.nextInt();
                reqNumb++;
                
                switch (choice) {
                    case 1:
                        msg = new Message(Type.REGISTER, reqNumb, clientName, clientAddress, clientPort);
                        msg.send(serverAddress, serverPort, socket);
                        break;

                    case 2:
                        File[] files = directory.listFiles();
                        for (int i = 0; i < files.length; i++) {
                            filenamesToPublish.add(files[i].getName());
                        }
                        msg = new Message(Type.PUBLISH, reqNumb, clientName, filenamesToPublish);
                        msg.send(serverAddress, serverPort, socket);
                        break;

                    case 3:
                        msg = new Message(Type.DEREGISTER, reqNumb, clientName);
                        msg.send(serverAddress, serverPort, socket);
                        break;
                        
                    case 4:
                        filenamesToRemove.add("Main.java");
                        filenamesToRemove.add("UDPClient.java");
                        filenamesToRemove.add("UDPServer.java");
                        msg = new Message(Type.REMOVE, reqNumb, clientName, filenamesToRemove);
                        msg.send(serverAddress, serverPort, socket);
                        break;

                    case 5:
                        if(clientRegistered) {
                            // System.out.print("Enter IP address of destination client: ");
                            // String stringAddress = scanner.next();
                            // peerAddress = InetAddress.getByName(stringAddress);

                            // System.out.print("Enter dest UDP port for destination client: ");
                            // int peerPort = scanner.nextInt();

                            // System.out.print("Enter requested filename: ");
                            // String filename = scanner.next();
                            
                            Thread listenerThread = new Thread(new p2pSetup());
                            listenerThread.start();
                            // Send FILE_REQ message
                            // Message msg = new Message(Type.FILE_REQ, reqNumb, filename);
                            // msg.send(peerAddress, Integer.valueOf(peerPort), socket);                       
                        } else {
                            System.out.println("Client must be registered to the server");
                        }
                        break;

                    case 6:
                        break loop;

                    default:
                        System.out.println("Invalid choice!");
                }
                
                // Clear buffer
                buffer = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
                Message messageReceived = (Message)ois.readObject();
                System.out.println("\nClient received " + messageReceived.toString());

                if(messageReceived.getType().equals(Type.REGISTERED)) {
                    clientRegistered = true;  
                } else if (messageReceived.getType().equals(Type.DEREGISTERED)) {
                    clientRegistered = false;
                }
            }
            scanner.close();
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout occured: Server did not respond.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    static class p2pSetup implements Runnable {
        private static final String HOST = "127.0.0.1";
        private static int UDP_PORT;
        private static final int BUFFER_SIZE = 65535;
        // Wait for file over provided TCP socket number
        @Override
        public void run() { 
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
                    Thread fileReceiver = new Thread(new FileReceiver(messageReceived.getSocket()));
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
}    
//             try {
//                 while(true){
//                     byte[] buffer = new byte[BUFFER_SIZE];
//                     DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
//                     // Receive packet
//                     socket.receive(receivePacket);
//                     ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
//                     Message messageReceived = (Message)ois.readObject();
//                     System.out.println("\nClient received " + messageReceived.toString());
    
//                     // Extract sender information
//                     InetAddress senderIp = receivePacket.getAddress();
//                     int senderUdpPort = receivePacket.getPort();
    
//                     // Client received a file request
//                     if(messageReceived.getType().equals(Type.FILE_REQ)) { // ADD CHECK TO SEE IF CLIENT OWNS THE FILE BEFORE SENDING FILE_CONF
    
//                         int tcpSocketNumber = acquireTCPSocketNumber(); // Get a random TCP socket number
//                         Message fileConfMessage = new Message(Type.FILE_CONF, messageReceived.getRq(), tcpSocketNumber); // Send a file conf message to the client who requested the file
//                         fileConfMessage.send(senderIp, senderUdpPort, socket);
    
//                         // Send file over provided TCP socket
//                         Thread fileSender = new Thread(new FileSender(senderIp, tcpSocketNumber, messageReceived.getRq(), messageReceived.getName()));
//                         fileSender.start();
//                     }
    
//                     // Client received a file conf
//                     if(messageReceived.getType().equals(Type.FILE_CONF)) {
//                         Thread fileReceiver = new Thread(new FileReceiver(messageReceived.getSocket()));
//                         fileReceiver.start();
//                     }
//                 }
    
//             } catch(Exception e) {
//                 e.printStackTrace();
//             }
//         }
//     }

//     public static int acquireTCPSocketNumber() {
//         final int MIN_PORT_NUMBER = 1025;
//         final int MAX_PORT_NUMBER = 65535;
//         Random r = new Random();
//         return r.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
//     }

//     static class FileReceiver implements Runnable {
//         private int socketNumber;

//         FileReceiver(int socketNumber) {
//             this.socketNumber = socketNumber;
//         }

//         // Wait for file over provided TCP socket number
//         @Override
//         public void run() {
//             try {
//                 Socket tcpSocket = new Socket(InetAddress.getByName("localhost"), socketNumber);
//                 System.out.println("Client waiting for file");
//                 BufferedWriter bw = new BufferedWriter(new FileWriter("temp.txt"));
//                 Message file = null;
//                 while (file == null || !file.getType().equals(Type.FILE_END)) {
//                     // ADD TIMEOUT IF CLIENT DOES NOT RECEIVE ANYTHING
//                     file = Message.receive(tcpSocket);
//                     System.out.println("Client received " + file.toString());
//                     // Put file back together
//                     bw.write(file.getText());
//                     bw.flush();
//                 }
//                 String filename = file.getName();
//                 // RENAME AND ADD TO LIST OF OWNED FILE
//                 bw.close();
//                 tcpSocket.close();
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//         }
//     }

//     static class FileSender implements Runnable {
//         private static final int CONNECTION_DELAY = 60000;
//         private static final int MAX_CHAR = 200;
//         private static final int EOF = -1;
//         private static final int FILE_OFFSET = 0;
//         private static final int BACKLOG_SIZE = 1;
//         private InetAddress destinationIp;
//         private int socketNumber;
//         private int rq;
//         private String filename;

//         public FileSender(InetAddress destinationIp, int socketNumber, int rq, String filename) {
//             this.destinationIp = destinationIp;
//             this.socketNumber = socketNumber;
//             this.filename = filename;
//             this.rq = rq;
//         }

//         // Send requested file over provided TCP socket
//         @Override
//         public void run() {
//             try  {
//                 // Accept TCP connection
//                 ServerSocket server = new ServerSocket(socketNumber, BACKLOG_SIZE, destinationIp);
//                 server.setSoTimeout(CONNECTION_DELAY);
//                 Socket destination = server.accept();
//                 // Setup to read textFile character by character
//                 TextFile textFile = new TextFile(new File(filename));
//                 FileReader fr = new FileReader(textFile.getFile());
//                 BufferedReader br = new BufferedReader(fr);
//                 char[] buffer = new char[MAX_CHAR];
//                 int ch;
//                 int chunk = 1;
//                 Message msg = new Message(Type.FILE, rq, filename);
//                 // Read the textFile character by character
//                 while ((ch = br.read(buffer, FILE_OFFSET, buffer.length)) != EOF) {
//                     String text = new String(buffer, FILE_OFFSET, ch); // Get the text
//                     msg.setText(text);
//                     msg.setChunk(chunk); // Update the message attributes
//                     chunk++;
//                     if(text.toCharArray().length < MAX_CHAR) { // Special END_FILE message for last chunk
//                         msg.setType(Type.FILE_END);
//                     }
//                     msg.send(destination); // Send the message over TCP socket
//                 }
//                 // Close the sockets created to receive the file
//                 destination.close();
//                 server.close();
//             } catch(SocketTimeoutException e){
//                 System.err.println("Timeout waiting for client connection");
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//         }
//     }
// }