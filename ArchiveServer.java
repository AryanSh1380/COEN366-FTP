import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Semaphore;

public class ArchiveServer {
    static private List<Client> clients = new ArrayList<>();
    static private DatagramSocket socket;
    static private Semaphore s = new Semaphore(1);
    static private InetAddress serverAddress;
    static private Integer serverPort;
    static private Timer timer;
    static private long timerPeriod = 10000;
    static private int archiveServerPort = 9999;
	private static final Integer BUFFER_SIZE = 65535;

    public static void main(String[] args) {
        try {
            socket = new DatagramSocket(archiveServerPort);
			byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            timer = new Timer();
            while(!socket.isClosed()) {
                System.out.println("Archive Server is running at: " + InetAddress.getLocalHost().toString() + ":" + archiveServerPort);
                
                socket.receive(request);
                byte[] receivedData = request.getData();
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivedData));
                Message msg = (Message) in.readObject();

                serverAddress = request.getAddress();
                serverPort = request.getPort();

                if(msg.getType() == Type.UPDATE) {
                    clients = msg.getClients();
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            ArchiveServer.updateServer(socket);
                        }
                    }, 0, timerPeriod);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            s.release();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    private static void updateServer(DatagramSocket socket) {
        try {
            Message updateClient = new Message(Type.UPDATE_SERVER, clients);
            System.out.println(updateClient.toString());
            updateClient.send(serverAddress, serverPort, socket);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
