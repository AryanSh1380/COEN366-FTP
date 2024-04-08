import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class TCPServer {
    /*
     * private static final int PORT = 5555;
     * 
     * public static void main(String[] args) {
     * try {
     * SetupUDPConnection();
     * } catch (Exception e) {
     * System.out.println(e.getMessage());
     * }
     * }
     * 
     * public static boolean SetupUDPConnection() throws Exception {
     * DatagramSocket socket = null;
     * try {
     * socket = new DatagramSocket();
     * socket.setSoTimeout(5000);
     * InetAddress serverAddress = InetAddress.getByName("localhost");
     * int serverPort = PORT;
     * } catch(Exception e) {
     * System.out.println(e.getMessage());
     * }
     * return true;
     * }
     * 
     * public static boolean SetupTCPConnection() {
     * ServerSocket serverSocket = null;
     * try {
     * serverSocket = new ServerSocket(PORT);
     * System.out.println("Server listening on port: " + PORT);
     * while (true) {
     * Socket clientSocket = serverSocket.accept();
     * System.out.println("Connection from " +
     * clientSocket.getInetAddress().getHostAddress());
     * new ClientHandler(clientSocket).start();
     * }
     * } catch (IOException e) {
     * e.printStackTrace();
     * } finally {
     * try {
     * if (serverSocket != null) {
     * serverSocket.close();
     * }
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * }
     * return true;
     * }
     * 
     * static class ClientHandler extends Thread {
     * private Socket clientSocket;
     * 
     * public ClientHandler(Socket clientSocket) {
     * this.clientSocket = clientSocket;
     * }
     * 
     * public void run() {
     * try {
     * Message clientMessage = Message.receive(clientSocket);
     * System.out.println(clientMessage.toString());
     * clientSocket.close();
     * } catch (IOException e) {
     * e.printStackTrace();
     * } catch (ClassNotFoundException e) {
     * e.printStackTrace();
     * }
     * 
     * }
     * 
     * private void sendToArchiveServer () {
     * try {
     * Socket archiveServerSocket = new Socket("127.0.0.1", 6666);
     * PrintWriter archiveOut = new
     * PrintWriter(archiveServerSocket.getOutputStream(), true);
     * /*
     * // Debuggion: print the content of ClientInfoList
     * System.out.println("Sending client information to archive server: ");
     * for (String clientInfo : clientInfoList) {
     * System.out.println(clientInfo);
     * }
     * 
     * // Send client information to the archive server
     * for (String clientInfo : clientInfoList) {
     * archiveOut.println(clientInfo);
     * }
     */
}