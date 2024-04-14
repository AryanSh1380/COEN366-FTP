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
import java.io.ObjectInputStream;

public class UDPClient {

    // Attribute for communication with server and other clients
    private static Client client;
    private static InetAddress serverAddress;
    private static byte[] buffer;
    private static DatagramSocket datagramSocket;
    private static List<Client> peers = new ArrayList<>();
    private static final Integer BUFFER_SIZE = 65535;
    private static final Integer SERVER_PORT = 2308;
    private final static Integer MIN_PORT_NUMBER = 1025;
    private final static Integer MAX_PORT_NUMBER = 65535;

    public static Integer acquireTCPSocketNumber() {
        Random r = new Random();
        return r.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
    }

    public static Boolean isPeer(InetAddress ip, Integer port) {
        Boolean isPeer = false;
        if(!peers.isEmpty()){
            for(Client peer : peers) {
                System.out.println("Provided: " + ip.getHostName() + " " + port);
                System.out.println("Peerlist " + peer.getClientAddress() + " " + peer.getClientPort());
                if((peer.getClientAddress() == ip) && (peer.getClientPort() == port)){
                    isPeer = true;
                }
            }
        }
        return isPeer;
    }

    public static void main(String[] args) {

        try {

            // Initialize client information
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter client name: ");
            String clientName = sc.nextLine();
            System.out.println("Enter UDP port for the client: ");
            Integer clientPort = sc.nextInt();
            sc.nextLine();
            client = new Client(clientName, InetAddress.getLocalHost(), clientPort);

            // Get server address
            System.out.println("Enter server address: ");
            serverAddress = InetAddress.getByName(sc.nextLine());

            // Start listening to port for incoming UDP packets
            datagramSocket = new DatagramSocket(client.getClientPort());
            //datagramSocket.setSoTimeout(50000);
            buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            System.out.println("Client listening to port " + client.getClientPort());

            // Display menu options
            System.out.println("Select an option:");
            System.out.println("1. REGISTER");
            System.out.println("2. PUBLISH");
            System.out.println("3. DEREGISTER");
            System.out.println("4. REMOVE");
            System.out.println("5. P2P");
            System.out.println("6. UPDATE");
            System.out.println("7. EXIT");
            System.out.print("Enter your choice: ");

            // Launch thread to perform actions
            Thread menu = new Thread(new InteractiveMenu(datagramSocket, client));
            menu.start();

            // Continuously listen to the client port
            while (!datagramSocket.isClosed()) {

                // Receive incoming packets
                datagramSocket.receive(receivePacket);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
                Message messageReceived = (Message) ois.readObject();
                System.out.println("\nClient received " + messageReceived.toString());

                // Extract sender information
                InetAddress senderIp = receivePacket.getAddress();
                Integer senderUdpPort = receivePacket.getPort();


                // Client received a file request
                if(messageReceived.getType().equals(Type.FILE_REQ)) {

                    // Check if client requesting the file is registered
                    /*
                    if(!isPeer(senderIp, senderUdpPort)) {
                        Message fileErrorMessage = new Message(Type.FILE_ERROR, messageReceived.getRq(), Reason.UNKNOWN_CLIENT);
                        fileErrorMessage.send(senderIp, senderUdpPort, datagramSocket);
                    // Check if file has been published by the client
                    } else if(!client.isPublished(messageReceived.getName())) {
                        Message fileErrorMessage = new Message(Type.FILE_ERROR, messageReceived.getRq(), Reason.FILE_NOT_FOUND);
                        fileErrorMessage.send(senderIp, senderUdpPort, datagramSocket);
                    } else {
                        // Accept the request and send a file configuration message
                        Integer tcpSocketNumber = acquireTCPSocketNumber();
                        Message fileConfMessage = new Message(Type.FILE_CONF, messageReceived.getRq(), tcpSocketNumber);
                        fileConfMessage.send(senderIp, senderUdpPort, datagramSocket);

                        // Send the file over provided TCP socket
                        Thread fileSender = new Thread(new FileSender(senderIp, tcpSocketNumber, messageReceived.getRq(), messageReceived.getName()));
                        fileSender.start();
                    }
                     */
                    // Accept the request and send a file configuration message
                    Integer tcpSocketNumber = acquireTCPSocketNumber();
                    Message fileConfMessage = new Message(Type.FILE_CONF, messageReceived.getRq(), tcpSocketNumber);
                    fileConfMessage.send(senderIp, senderUdpPort, datagramSocket);

                    // Send the file over provided TCP socket
                    Thread fileSender = new Thread(new FileSender(senderIp, tcpSocketNumber, messageReceived.getRq(), messageReceived.getName()));
                    fileSender.start();
                }

                // Client received a file configuration message
                if(messageReceived.getType().equals(Type.FILE_CONF)) {
                    Thread fileReceiver = new Thread(new FileReceiver(messageReceived.getSocketNum(), senderIp));
                    fileReceiver.start();
                }

                // Client received an update message
                if(messageReceived.getType().equals(Type.UPDATE)){
                    peers = messageReceived.getClients();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }
    }

    static class InteractiveMenu implements Runnable {
        private DatagramSocket datagramSocket;
        private Client client;
        private List<String> filenamesToPublish;
        private List<String> filenamesToRemove;
        private File directory = new File(".");
        private InetAddress updatedClientAddress;
        private Integer updatedClientPort;
        private Integer fileCount = 0;
        private Integer requestNumber = 0;

        InteractiveMenu(DatagramSocket datagramSocket, Client client){
            this.datagramSocket = datagramSocket;
            this.client = client;
        }

        public Integer incrementRequestNumber() {
            return requestNumber++;
        }

        @Override
        public void run() {

            try {
                Scanner scanner = new Scanner(System.in);
                File[] files = directory.listFiles();
                loop:
                while (!datagramSocket.isClosed()) {
                    
                    // Reading user input
                    int choice = scanner.nextInt();
                    scanner.nextLine();
                    incrementRequestNumber();

                    switch (choice) {
                        case 1:
                            Message msg = new Message(Type.REGISTER, requestNumber, client.getClientName(), client.getClientAddress(), client.getClientPort());
                            msg.send(serverAddress, SERVER_PORT, datagramSocket);
                            break;

                        case 2:
                            filenamesToPublish = new ArrayList<>();
                            System.out.println("How many files would you like to add: ");
                            fileCount = scanner.nextInt();
                            scanner.nextLine(); // Consume newline character
                            while (fileCount > 0) {
                                System.out.println("Enter filename: ");
                                String filename = scanner.nextLine();
                                boolean fileExists = false;
                                for (File file : files) {
                                    if (filename.equals(file.getName())) {
                                        filenamesToPublish.add(filename);
                                        fileExists = true;
                                        break;
                                    }
                                }
                                if (!fileExists) {
                                    System.out.println("Invalid input: File must exist in the same directory as the client");
                                } else {
                                    fileCount--;
                                }
                            }
                            msg = new Message(Type.PUBLISH, requestNumber, client.getClientName(), filenamesToPublish);
                            msg.send(serverAddress, SERVER_PORT, datagramSocket);
                            break;

                        case 3:
                            msg = new Message(Type.DEREGISTER, requestNumber, client.getClientName());
                            msg.send(serverAddress, SERVER_PORT, datagramSocket);
                            break;

                        case 4:
                            filenamesToRemove = new ArrayList<>();
                            System.out.println("How many files would you like to remove: ");
                            fileCount = scanner.nextInt();
                            scanner.nextLine(); // Consume newline character
                            while (fileCount > 0) {
                                System.out.println("Enter filename: ");
                                String filename = scanner.nextLine();
                                boolean fileExists = false;
                                for (File file : files) {
                                    if (filename.equals(file.getName())) {
                                        filenamesToRemove.add(filename);
                                        fileExists = true;
                                        break;
                                    }
                                }
                                if (!fileExists) {
                                    System.out.println("Invalid input: File must exist in the same directory as the client");
                                } else {
                                    fileCount--;
                                }
                            }
                            msg = new Message(Type.REMOVE, requestNumber, client.getClientName(), filenamesToRemove);
                            msg.send(serverAddress, SERVER_PORT, datagramSocket);
                            break;

                        case 5:
                            System.out.println("Enter the IP address and the UDP socket of the client and the file name to send a file request");
                            String peerIp = scanner.nextLine();
                            int peerPort = scanner.nextInt();
                            scanner.nextLine();
                            String filename = scanner.nextLine();

                            // Send FILE_REQ message
                            Message fileReq = new Message(Type.FILE_REQ, requestNumber, filename);
                            fileReq.send(InetAddress.getByName(peerIp), peerPort, datagramSocket);
                            break;

                        case 6:
                            System.out.println("Select an option:");
                            System.out.println("1. Update IP Address");
                            System.out.println("2. Update port number");
                            System.out.println("3. Update both IP address and port number");
                            System.out.print("Enter your choice: ");
                            int request = scanner.nextInt();

                            switch (request) {
                                case 1:
                                    System.out.println("Enter new IP address: ");
                                    String newAddress = scanner.next();
                                    updatedClientAddress = InetAddress.getByName(newAddress);
                                    msg = new Message(Type.UPDATE, requestNumber, client.getClientName(), updatedClientAddress, client.getClientPort());
                                    msg.send(serverAddress, SERVER_PORT, datagramSocket);
                                    break;
                                    
                                case 2:
                                    System.out.println("Enter new port number: ");
                                    updatedClientPort = scanner.nextInt();
                                    msg = new Message(Type.UPDATE, requestNumber, client.getClientName(), client.getClientAddress(), updatedClientPort);
                                    msg.send(serverAddress, SERVER_PORT, datagramSocket);
                                    break;

                                case 3:
                                    System.out.println("Enter new IP address: ");
                                    newAddress = scanner.next();
                                    updatedClientAddress = InetAddress.getByName(newAddress);
                                    System.out.println("Enter new port number: ");
                                    updatedClientPort = scanner.nextInt();
                                    scanner.nextLine();
                                    msg = new Message(Type.UPDATE, requestNumber, client.getClientName(), client.getClientAddress(), updatedClientPort);
                                    msg.send(serverAddress, SERVER_PORT, datagramSocket);
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case 7:
                            break loop;
                        default:
                            System.out.println("Invalid choice!");
                    }
                }
                scanner.close();
            } catch(Exception e) {
                e.printStackTrace();
            }

        }
    }

    static class FileReceiver implements Runnable {
        private Integer socketNumber;
        private InetAddress destinationIp;

        FileReceiver(int socketNumber, InetAddress destinationIp) {
            this.socketNumber = socketNumber;
            this.destinationIp = destinationIp;
        }

        public String getUniqueFilename(String filename) {

            int fileVersion = 1;
            String uniqueFilename = filename;

            File file = new File(filename);

            if(file.exists()){
                // Extract the filename components
                String[] filenameSplit = filename.split("\\.");
                String name = filenameSplit[0];
                String dot = ".";
                String extension = filenameSplit[1];
                // Update the file version
                do {
                    uniqueFilename = name + "_" + fileVersion + dot + extension;
                    file = new File(uniqueFilename);
                    fileVersion++;
                } while(file.exists());
            }

            return uniqueFilename;
        }

        // Wait for file over provided TCP socket number
        @Override
        public void run() {
            try {
                // Connect to the TCP socket
                Socket tcpSocket = new Socket(destinationIp, socketNumber);
                System.out.println("Client waiting for file");

                // Receive the first message and get the filename
                Message fileMessage = Message.receive(tcpSocket);
                String filename = fileMessage.getName();
                System.out.println("Client received " + fileMessage);

                // Get a unique filename
                String uniqueFilename = getUniqueFilename(filename);

                // Create the file and write to it
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(uniqueFilename));
                bufferedWriter.write(fileMessage.getText());
                while(fileMessage.getType() != Type.FILE_END) {
                    fileMessage = Message.receive(tcpSocket);
                    System.out.println("Client received " + fileMessage.toString());
                    bufferedWriter.write(fileMessage.getText());
                    bufferedWriter.flush();
                }
                /*
                BufferedWriter bw = new BufferedWriter(new FileWriter("temp.txt"));
                Message fileMessage = null;
                while (fileMessage == null || !fileMessage.getType().equals(Type.FILE_END)) {
                    // ADD TIMEOUT IF CLIENT DOES NOT RECEIVE ANYTHING
                    fileMessage = Message.receive(tcpSocket);

                    // Put file back together
                    bw.write(fileMessage.getText());
                    bw.flush();
                }
                */
                bufferedWriter.close();
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
                // Accept the TCP connection
                ServerSocket sender = new ServerSocket(socketNumber, BACKLOG_SIZE, InetAddress.getLocalHost());
                sender.setSoTimeout(CONNECTION_DELAY);
                Socket receiver = sender.accept();

                // Setup to read text file character by character
                TextFile textFile = new TextFile(new File(filename));
                FileReader fr = new FileReader(textFile.getFile());
                BufferedReader br = new BufferedReader(fr);
                char[] buffer = new char[MAX_CHAR];
                int ch;
                int chunk = 1;

                // Initialize file message
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
                    msg.send(receiver); // Send the message over TCP socket
                }
                // Close the sockets created to send the file
                receiver.close();
                sender.close();
            } catch(SocketTimeoutException e){
                System.err.println("Timeout waiting for client connection");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}