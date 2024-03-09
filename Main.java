import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {

        List<TCPClient> clients = new ArrayList<>();
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(2308);
            byte[] buffer = new byte[1024];
            
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            socket.receive(request);

            String message = new String(request.getData(), 0, request.getLength());
            String[] parts = message.split("\\|");
        
            // parse register request: REGISTER | RQ# | ClientName | IP_Addr | socket#
            String commandType = parts[0].trim();
            Integer REQnumb = Integer.parseInt(parts[1].trim());
            String clientName = parts[2].trim();
            InetAddress clientAddress = request.getAddress();
            int clientPort = request.getPort();
            
            String response = "";
            if(commandType == "REGISTER") {
                boolean clientNameAlreadyExists = false;
                for (TCPClient client : clients) {
                    if (clientNameAlreadyExists) { //register denied
                        response = String.format("REGISTER-DENIED | %s | Name already taken by another Client", REQnumb);
                        clientNameAlreadyExists = true;
                    }
                }
                if (!clientNameAlreadyExists) {
                    //clients.add(new Client(clientName, clientAddress, clientPort));
                    response = String.format("REGISTERED | %s", REQnumb);
                }
            }
            
            byte[] sendData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);    
            socket.send(responsePacket);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
