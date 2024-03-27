import java.net.*;
import java.io.*;
import java.util.Scanner;

public class TCPClient {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int UDP_PORT = 5578;
    private static final int TCP_PORT = 3821;
    private static final int BUFFER_SIZE = 65535;

    public static void main(String[] args) {
        try {
            // UDP
            // Communication setup
            DatagramSocket ds = new DatagramSocket(UDP_PORT);
            byte[] buff = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buff, buff.length);
            System.out.println("Client listening to port " + UDP_PORT);

            Thread senderThread = new Thread(new SenderThread());
            senderThread.start();

            while(true){
                // Receive
                ds.receive(receivePacket); // Receive the packet
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
                Message msg = (Message)ois.readObject();
                System.out.println("Client received " + msg.toString());

                if(msg.getType().equals(Type.FILE_REQ)) {
                    // acquire TCP port
                    // reply with FILE_CONF
                }
                // Clear buffer
                buff = new byte[BUFFER_SIZE];
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private DatagramSocket socket;

        public ClientHandler(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

        }
    }

    static class SenderThread implements Runnable {
        private static int rq = 1;
        public int getNextRq() {
            return rq++;
        }
        @Override
        public void run() {

            try {

            // Communication setup
            Scanner sc = new Scanner(System.in);
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getByName(SERVER_HOST);
            byte buffer[] = null;
            System.out.println("Client ready to send");

            while (true) {

                // Trigger FILE_REQ message
                System.out.println("Enter the UDP socket of the client to send a FILE_REQ: ");
                String udpSocket = sc.nextLine();

                // FILE_REQ message
                Message msg = new Message(Type.FILE_REQ, rq, "hello_file.txt");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(msg);
                oos.flush();
                buffer = baos.toByteArray();

                // Send
                DatagramPacket sendingPacket = new DatagramPacket(buffer, buffer.length, ip, Integer.valueOf(udpSocket));
                ds.send(sendingPacket);
                getNextRq();

            }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

