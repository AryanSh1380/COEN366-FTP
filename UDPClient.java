import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;

public class UDPClient {
    private static Message msg;
    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            InetAddress clientAddress = InetAddress.getByName("localhost");
            int clientPort = 2308;

            Scanner scanner = new Scanner(System.in);
            File directory = new File(".");
            List<String> filenamesToPublish = new ArrayList<>();
            List<String> filenamesToRemove = new ArrayList<>();

            System.out.print("Enter Client Name: ");
            String clientName = scanner.nextLine();

            System.out.print("Enter Server address: ");
            String serverIP = scanner.nextLine();
            InetAddress serverAddress = InetAddress.getByName(serverIP);
            
            System.out.print("Enter Server Port Number: ");
            int serverPort = scanner.nextInt();

            int reqNumb = 0;
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
                reqNumb++;
                String message = "";
                
                switch (choice) {
                    case 1:
                        msg = new Message(Type.REGISTER, reqNumb, clientName, clientAddress, clientPort);
                        break;

                    case 2:
                        File[] files = directory.listFiles();
                        for (int i = 0; i < files.length; i++) {
                            filenamesToPublish.add(files[i].getName());
                        }
                        msg = new Message(Type.PUBLISH, reqNumb, clientName, filenamesToPublish);
                        break;

                    case 3:
                        msg = new Message(Type.DEREGISTER, reqNumb, clientName);
                        break;
                        
                    case 4:
                        filenamesToRemove.add("Main.java");
                        filenamesToRemove.add("UDPClient.java");
                        filenamesToRemove.add("UDPServer.java");
                        msg = new Message(Type.REMOVE, reqNumb, clientName, filenamesToRemove);
                        break;

                    case 5:
                        break loop;

                    default:
                        System.out.println("Invalid choice!");
                }
            
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(byteStream);
                out.writeObject(msg);
                out.flush();
                byte[] serializedObj = byteStream.toByteArray();
                
                DatagramPacket sendPacket = new DatagramPacket(serializedObj, serializedObj.length, serverAddress, serverPort);
                socket.send(sendPacket);

                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Server: " + receivedMessage);
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
}