import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class TCPClient {
    private static final String HOST = "127.0.0.1";
    private static int UDP_PORT;
    private static final int BUFFER_SIZE = 65535;

    private static final int MIN_PORT_NUMBER = 1025;
    private static final int MAX_PORT_NUMBER = 65535;

    private static int rq = 1;

    public static void main(String[] args) {
        try {

            Scanner sc = new Scanner(System.in);
            System.out.println("Enter UDP port for the client ");
            String port = sc.nextLine();
            UDP_PORT = Integer.valueOf(port);

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

                // Reply with FILE_CONF
                // First check if client owns the file
                if(msg.getType().equals(Type.FILE_REQ)) {

                    byte[] buffer = null;

                    // FILE_CONF message
                    int tcpPort = acquireTCPPort();
                    Message m = new Message(Type.FILE_CONF, msg.getRq(), tcpPort);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(m);
                    oos.flush();
                    buffer = baos.toByteArray();

                    // Send
                    DatagramPacket sendingPacket = new DatagramPacket(buffer, buffer.length, senderIp, senderUdpPort);
                    ds.send(sendingPacket);

                    sleep(1000);
                    // Send file over provided TCP socket
                    Socket sckt = new Socket(InetAddress.getByName(HOST), tcpPort);
                    TextFile file = new TextFile(msg.getName());
                    try  {
                        FileReader fr = new FileReader(file);
                        BufferedReader br = new BufferedReader(fr);
                        char[] buf = new char[200];
                        int ch;
                        Message mess = new Message(Type.FILE, msg.getRq(), msg.getName());
                        int chunk = 1;
                        while ((ch = br.read(buf, 0, buf.length)) != -1) {
                            // Process the content read
                            String text = new String(buf, 0, ch);
                            mess.setText(text);
                            mess.setChunk(chunk);
                            chunk++;
                            if(text.toCharArray().length < 200){
                                mess.setType(Type.FILE_END);
                            }
                            mess.send(sckt);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


                // Setup TCP communication on given port
                if(msg.getType().equals(Type.FILE_CONF)) {

                    // Wait for file over provided TCP socket
                    ServerSocket server = new ServerSocket(msg.getSocket(),1, senderIp);
                    Socket client = server.accept();
                    Message file = null;
                    while(file == null || !file.getType().equals(Type.FILE_END)){
                        file = Message.receive(client);
                        System.out.println("Client received " + file.toString());
                    }
                    client.close();
                    server.close();
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
                System.out.println("Enter the UDP socket of the client followed by the file name to send a FILE_REQ: ");
                String udpSocket = sc.nextLine();
                String filename = sc.nextLine();

                // FILE_REQ message
                Message msg = new Message(Type.FILE_REQ, rq, filename);
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

