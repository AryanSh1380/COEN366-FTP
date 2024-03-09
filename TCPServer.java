import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class TCPServer {
    private static final int PORT = 5555;
    private static final int MAX_CLIENT = 4;
    private static List<String> clientInfoList = new ArrayList<>();

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port: " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection from " + clientSocket.getInetAddress().getHostAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                //receive hello message from client
                String HelloMsg = in.readLine();
                if ("hello".equals(HelloMsg)) {
                    out.println("send_info");

                    // Receive Client information
                    String clientId = in.readLine();
                    String clientDesc = in.readLine();

                    // Print Client information 
                    System.out.println("Client " + clientId + " registered with description: " + clientDesc);

                    // Append Client information
                    clientInfoList.add(clientId + " " + clientDesc);

                    // If all clients registered, send iventory to archive server
                    if (clientInfoList.size() == MAX_CLIENT) {
                        sendToArchiveServer();
                        // Clear the list after sending to the archive server
                        clientInfoList.clear();
                    }
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }   

        private void sendToArchiveServer () {
            try {
                Socket archiveServerSocket = new Socket("127.0.0.1", 6666);
                PrintWriter archiveOut = new PrintWriter(archiveServerSocket.getOutputStream(), true);

                // Debuggion: print the content of ClientInfoList
                System.out.println("Sending client information to archive server: ");
                for (String clientInfo : clientInfoList) {
                    System.out.println(clientInfo);
                }

                // Send client information to the archive server
                for (String clientInfo : clientInfoList) {
                    archiveOut.println(clientInfo);
                }

                archiveOut.close();
                archiveServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
