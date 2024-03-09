import java.net.*;

public class UDPClient {
    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 2308;
            
            //String message1 = String.format("REGISTER | 1234 | ClientName | %s | %d", serverAddress, serverPort);
            String message1 = "hello";
            byte[] sendData = message1.getBytes();
            
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            socket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Server: " + receivedMessage);

            socket.close();
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout occured: Server did not respond.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}