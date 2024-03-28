import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Scanner;

public class TCPClient {
    private static final String HOST = "127.0.0.1";
    private static final int UDP_PORT = 5555;
    private static final int BUFFER_SIZE = 65535;

    private static final int MIN_PORT_NUMBER = 1025;
    private static final int MAX_PORT_NUMBER = 65535;

    private static int rq = 1;
    public static void main(String[] args) {
        try {

            // Listening to incoming messages on selected UDP port
            DatagramSocket ds = new DatagramSocket(UDP_PORT);
            byte[] buff = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buff, buff.length);
            System.out.println("Client listening to port " + UDP_PORT);

            // Start sender thread
            Thread senderThread = new Thread(new SenderThread(ds));
            senderThread.start();

            while(true){
                // Receive packet
                ds.receive(receivePacket);

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
                Message msg = (Message)ois.readObject();
                System.out.println("Client received " + msg.toString());

                // Client information
                InetAddress senderIp = receivePacket.getAddress();
                int senderUdpPort = receivePacket.getPort();
                System.out.println("Client address " + senderIp + " " + senderUdpPort);

                if(msg.getType().equals(Type.FILE_REQ)) {

                    // reply with FILE_CONF
                    byte[] buffer = null;

                    // FILE_CONF message
                    Message m = new Message(Type.FILE_CONF, msg.getRq(), acquireTCPPort());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(m);
                    oos.flush();
                    buffer = baos.toByteArray();

                    // Send
                    DatagramPacket sendingPacket = new DatagramPacket(buffer, buffer.length, senderIp, senderUdpPort);
                    ds.send(sendingPacket);

                }

                if(msg.getType().equals(Type.FILE_CONF)) {
                    // setup TCP communication on given port
                }

                // Clear buffer
                buff = new byte[BUFFER_SIZE];
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static int acquireTCPPort() {
        Random r = new Random();
        return r.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
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
        private DatagramSocket socket;

        public int getNextRq() {
            return rq++;
        }

        SenderThread(DatagramSocket datagramSocket){
            socket = datagramSocket;
        }

        @Override
        public void run() {

            try {

            // Communication setup
            Scanner sc = new Scanner(System.in);
            InetAddress ip = InetAddress.getByName(HOST); // change that
            byte[] buffer = null;
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
                socket.send(sendingPacket);
                getNextRq();

            }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

