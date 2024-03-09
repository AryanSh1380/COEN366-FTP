import java.net.*;
import java.io.*;

public class TCPClient {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5555;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //send hello message
            out.println("hello");

            //Receive request for client information
            String requestInfoMsg = in.readLine();
            if ("send_info".equals(requestInfoMsg)) {
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Enter Cleint ID: ");
                String ClientId = userInput.readLine();
                System.out.print("Enter client description: ");
                String ClientDesc = userInput.readLine();
                out.println(ClientId);
                out.println(ClientDesc);    
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
