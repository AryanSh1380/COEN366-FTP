import java.net.*;
import java.io.*;

public class TCPClient {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5555;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
