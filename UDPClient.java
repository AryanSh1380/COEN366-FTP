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
    // Client attributes
    private String clientName;
    private InetAddress clientAddress;
    private Integer clientPort;
    private List<String> listOfFiles;
    private Integer requestNumber;

    // For communication with server and other clients
    private static InetAddress serverAddress;
    private static Integer serverPort;
    private static byte[] buffer;
    private static final int BUFFER_SIZE = 65535;
    private static DatagramSocket datagramSocket;

    public UDPClient() throws UnknownHostException {
        listOfFiles = new ArrayList<>();
        this.clientName = "";
        this.clientAddress = InetAddress.getLocalHost();
        this.clientPort = 5555;
        this.requestNumber = 0;
    }

    public void addFileToList(String fileName) {
        listOfFiles.add(fileName);
    }

    public void removeFileFromList(String fileName) {
        listOfFiles.remove(fileName);
    }

    public void printFileNameFromList() {
        for (String filename : listOfFiles) {
            System.out.println(filename);
        }
    }

    public String getClientName() {
        return clientName;
    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public Integer getClientPort() {
        return clientPort;
    }

    public Integer getClientRequestNumber() {
        return requestNumber;
    }

    public Integer incrementRequestNumber(){
        return requestNumber++;
    }
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setClientAddress(InetAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    public void setClientPort(Integer clientPort) {
        this.clientPort = clientPort;
    }

    public void setListOfFiles(List<String> listOfFiles) {
        this.listOfFiles = listOfFiles;
    }

    public static int acquireTCPSocketNumber() {
        final int MIN_PORT_NUMBER = 1025;
        final int MAX_PORT_NUMBER = 65535;
        Random r = new Random();
        return r.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
    }


    public static void main(String[] args) {

        try {
            // Initialize client information
            UDPClient cl = new UDPClient();
            System.out.println("Client address is " + cl.getClientAddress().getCanonicalHostName());
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter client name: ");
            cl.setClientName(sc.nextLine());
            System.out.println("Enter UDP port for the client: ");
            cl.setClientPort(sc.nextInt());
            sc.nextLine();
            System.out.println("Enter server address: ");
            serverAddress = InetAddress.getByName(sc.nextLine());
            System.out.println("Enter server UDP port: ");
            serverPort = sc.nextInt();
            sc.nextLine();
            // Start listening to port for incoming UDP packets
            datagramSocket = new DatagramSocket(cl.getClientPort());
            //datagramSocket.setSoTimeout(50000);
            buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            System.out.println("Client listening to port " + cl.getClientPort());

            // Displaying menu options
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
            Thread menu = new Thread(new InteractiveMenu(datagramSocket, cl));
            menu.start();

            // Receive incoming packets
            while (!datagramSocket.isClosed()) {
                datagramSocket.receive(receivePacket);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
                Message messageReceived = (Message) ois.readObject();
                System.out.println("\nClient received " + messageReceived.toString());

                // Process the received messages

                // Extract sender information
                InetAddress senderIp = receivePacket.getAddress();
                int senderUdpPort = receivePacket.getPort();

                // Test with server
                if(messageReceived.getType().equals(Type.REGISTERED)) {
                    System.out.println("Client registered");
                }

                // Client received a file request
                if(messageReceived.getType().equals(Type.FILE_REQ)) { // ADD CHECK TO SEE IF CLIENT OWNS THE FILE BEFORE SENDING FILE_CONF

                    int tcpSocketNumber = acquireTCPSocketNumber(); // Get a random TCP socket number
                    Message fileConfMessage = new Message(Type.FILE_CONF, messageReceived.getRq(), tcpSocketNumber); // Send a file conf message to the client who requested the file
                    fileConfMessage.send(senderIp, senderUdpPort, datagramSocket);

                    // Send file over provided TCP socket
                    Thread fileSender = new Thread(new FileSender(senderIp, tcpSocketNumber, messageReceived.getRq(), messageReceived.getName()));
                    fileSender.start();
                }

                // Client received a file conf
                if(messageReceived.getType().equals(Type.FILE_CONF)) {
                    Thread fileReceiver = new Thread(new FileReceiver(messageReceived.getSocket(), senderIp));
                    fileReceiver.start();
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
        private UDPClient client;
        private List<String> filenamesToPublish;
        private List<String> filenamesToRemove;
        private File directory = new File(".");
        private InetAddress updatedClientAddress;
        private Integer updatedClientPort;
        private Integer fileCount = 0;

        InteractiveMenu(DatagramSocket datagramSocket, UDPClient client){
            this.datagramSocket = datagramSocket;
            this.client = client;
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
                    client.incrementRequestNumber();

                    switch (choice) {
                        case 1:
                            Message msg = new Message(Type.REGISTER, client.getClientRequestNumber(), client.getClientName(), client.getClientAddress(), client.getClientPort());
                            msg.send(serverAddress, serverPort, datagramSocket);
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
                            msg = new Message(Type.PUBLISH, client.getClientRequestNumber(), client.getClientName(), filenamesToPublish);
                            msg.send(serverAddress, serverPort, datagramSocket);
                            break;

                        case 3:
                            msg = new Message(Type.DEREGISTER, client.getClientRequestNumber(), client.getClientName());
                            msg.send(serverAddress, serverPort, datagramSocket);
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
                            msg = new Message(Type.REMOVE, client.getClientRequestNumber(), client.getClientName(), filenamesToRemove);
                            msg.send(serverAddress, serverPort, datagramSocket);
                            break;

                        case 5:
                            System.out.println("Enter the IP address and the UDP socket of the client and the file name to send a file request");
                            String peerIp = scanner.nextLine();
                            int peerPort = scanner.nextInt();
                            scanner.nextLine();
                            String filename = scanner.nextLine();

                            // Send FILE_REQ message
                            Message fileReq = new Message(Type.FILE_REQ, client.getClientRequestNumber(), filename);
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
                                    msg = new Message(Type.UPDATE, client.getClientRequestNumber(), client.getClientName(), updatedClientAddress, client.getClientPort());
                                    msg.send(serverAddress, serverPort, datagramSocket);
                                    break;
                                    
                                case 2:
                                    System.out.println("Enter new port number: ");
                                    updatedClientPort = scanner.nextInt();
                                    msg = new Message(Type.UPDATE, client.getClientRequestNumber(), client.getClientName(), client.getClientAddress(), updatedClientPort);
                                    msg.send(serverAddress, serverPort, datagramSocket);
                                    break;

                                case 3:
                                    System.out.println("Enter new IP address: ");
                                    newAddress = scanner.next();
                                    updatedClientAddress = InetAddress.getByName(newAddress);
                                    System.out.println("Enter new port number: ");
                                    updatedClientPort = scanner.nextInt();
                                    scanner.nextLine();
                                    msg = new Message(Type.UPDATE, client.getClientRequestNumber(), client.getClientName(), client.getClientAddress(), updatedClientPort);
                                    msg.send(serverAddress, serverPort, datagramSocket);
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
        private int socketNumber;
        private InetAddress destinationIp;

        FileReceiver(int socketNumber, InetAddress destinationIp) {
            this.socketNumber = socketNumber;
            this.destinationIp = destinationIp;
        }

        // Wait for file over provided TCP socket number
        @Override
        public void run() {
            try {
                Socket tcpSocket = new Socket(destinationIp, socketNumber);
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
                ServerSocket server = new ServerSocket(socketNumber, BACKLOG_SIZE, InetAddress.getLocalHost());
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

}