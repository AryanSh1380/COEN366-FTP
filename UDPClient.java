import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;

public class UDPClient {
    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 2308;

            Scanner scanner = new Scanner(System.in);
            File directory = new File(".");
            List<String> filenames = new ArrayList<>();
            
            loop: while(!socket.isClosed()) {
                // Displaying menu options
                System.out.println("Select an option:");
                System.out.println("1. REGISTER");
                System.out.println("2. PUBLISH");
                System.out.println("3. DEREGISTER");
                System.out.println("4. REMOVE");
                System.out.println("5. EXIT");
                System.out.print("Enter your choice: ");

                // Reading user input
                int choice = scanner.nextInt();
                
                String message = "";

                switch (choice) {
                    case 1:
                        message = String.format("REGISTER | 6 | Client6 | NAME | | %s | %d", serverAddress, serverPort);
                        System.out.println("message: " + message);
                        break;

                    case 2:
                        File[] files = directory.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile()) {
                                    filenames.add(file.getName());
                                }
                            }
                        }
                        
                        StringBuilder concatenatedFilenames = new StringBuilder();
                        for (int i = 0; i < filenames.size(); i++) {
                            concatenatedFilenames.append(filenames.get(i));
                            if (i < filenames.size() - 1) {
                                concatenatedFilenames.append(" | ");
                            }
                        }

                        message = String.format("PUBLISH | 6 | Client6 | FileName1 | %s",concatenatedFilenames.toString());
                        System.out.println("message: " + message);
                        break;
                    case 3:
                        message = String.format("DEREGISTER | 6 | Client6");
                        System.out.println("message: " + message);
                        break;
                    
                    case 4:
                        String fileToRemove = "Main.java | UDPClient.java | UDPServer.java"; 
                        message = String.format("REMOVE | 6 | Client6 | %s", fileToRemove);
                        System.out.println("message: " + message);
                        break;

                    case 5:
                        break loop;

                    default:
                        System.out.println("Invalid choice!");
                }
            
                byte[] sendData = message.getBytes();
                
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                socket.send(sendPacket);

                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Server: " + receivedMessage);
            }
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
}