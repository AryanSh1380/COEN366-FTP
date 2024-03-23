import java.net.*;
import java.io.*;

public class TCPClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5555;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(HOST, PORT);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean SetupUDPConnection() throws Exception {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = PORT;
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        return true;
    }

    public static boolean InitiateUDPConnection
}
