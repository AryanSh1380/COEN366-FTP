import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

public class Message implements Serializable {

    // Server to client and client to server
    private Type type;
    private int rq;
    private Reason reason;
    private String name;
    private InetAddress ipAddress;
    private int socketNum;
    private int UDPport;
    private List<String> filenames;

    // Client to client
    private int chunk;
    private String text;

    public int getUDPport() {
        return UDPport;
    }

    public Reason getReason() {
        return reason;
    }

    public String getName() {
        return name;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public int getSocket() {
        return socketNum;
    }

    public List<String> getFiles() {
        return filenames;
    }

    public Type getType() {
        return type;
    }

    public int getRq() {
        return rq;
    }

    public String getText(){
        return text;
    }

    public int getChunk(){
        return chunk;
    }

    void setType(Type type) {
        this.type = type;
    }
    public void setText(String text) {
        this.text = text;
    }

    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    // [REGISTERED | RQ#] (server)
    // [PUBLISHED | RQ#] (server)
    // [REMOVED | RQ#]  (server)
    public Message(Type type, int rq) {
        this.type = type;
        this.rq = rq;
    }

    // [DE-REGISTER | RQ# | Name] (client)
    // [FILE-REQ | RQ# | Filename] (client)
    public Message(Type type, int rq, String name) {
        this.type = type;
        this.rq = rq;
        this.name = name;
    }

    // [PUBLISH-DENIED | RQ# | Reason] (server)
    // [FILE-ERROR | RQ# | Reason] (server)
    public Message(Type type, int rq, Reason reason) {
        this.type = type;
        this.rq = rq;
        this.reason = reason;
    }

    // [REGISTER | RQ# | Name | IP Address | UDP socket#] (client)
    public Message(Type type, int rq, String name, InetAddress ipAddress, int socketNum) {
        this.type = type;
        this.rq = rq;
        this.name = name;
        this.ipAddress = ipAddress;
        this.socketNum = socketNum;
    }

    // [PUBLISH | RQ# | Name | List of files] (client) 
    // [REMOVE | RQ# | Name | List of files] (client) 
    public Message(Type type, int rq, String name, List<String> filenames) {
        this.type = type;
        this.rq = rq;
        this.name = name;
        this.filenames = filenames;
    }

    // [UPDATE-DENIED | RQ# | Reason] (server)
    public Message(Type type, int rq, Reason reason, String name) {
        this.type = type;
        this.rq = rq;
        this.reason = reason;
        this.name = name;
    }

    // [FILE-CONF | RQ# | TCP socket#]
    public Message(Type type, int rq, int socketNum) {
        this.type = type;
        this.rq = rq;
        this.socketNum = socketNum;
    }

    public Message(Type type, int rq, String name, int chunk, String text) {
        this.type = type;
        this.rq = rq;
        this.name = name;
        this.chunk = chunk;
        this.text = text;
    }

    // Send message over TCP
    public void send(Socket socket) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(this);
        out.flush();

    }

    // Send message over UDP
    public void send(InetAddress ip, int port, DatagramSocket socket) throws IOException {
        // Setup
        byte[] buffer = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.flush();
        buffer = baos.toByteArray();
        // Send
        DatagramPacket sendingPacket = new DatagramPacket(buffer, buffer.length, ip, port);
        socket.send(sendingPacket);
    }

    // Receive message over TCP
    public static Message receive(Socket socket) throws IOException, ClassNotFoundException {
        Message msg = null;
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            msg = (Message) in.readObject();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return msg;
    }

    @Override
    public String toString() {
        String start = type + " | " + rq;
        switch(type) {
            case REGISTER: {
                return start + " | " + name + " | " + ipAddress + " | " + socketNum;
            }
                
            case REGISTERED:
                return start;
            case REGISTER_DENIED:
                return start + " | " + reason;
            case DEREGISTER:
                return start + " | " + name;
            case PUBLISH:
                return start + " | " + name + " | " + filenames;
            case PUBLISHED:
                return start;
            case PUBLISH_DENIED:
                return start + " | " + reason;
            case REMOVE:
                return start + " | " + name + " | " + filenames;
            case REMOVED:
                return start;
            case REMOVE_DENIED:
                return start + " | " + reason;
            case UPDATE:
                return start + " | " + name + " | " + ipAddress + " | " + socketNum + " | " + filenames;
            case UPDATE_CONTACT:
                return start + " | " + name + " | " + ipAddress + " | " + socketNum;
            case UPDATE_CONFIRMED:
                return start + " | " + name + " | " + ipAddress + " | " + socketNum;
            case UPDATE_DENIED:
                return start + " | " + name + " | " + reason;
            case FILE_REQ:
                return start + " | " + name;
            case FILE_CONF:
                return start + " | " + socketNum;
            case FILE:
                return start + " | " + name + " | " + chunk + " | " + text;
            case FILE_END:
                return start + " | " + name + " | " + chunk + " | " + text;
            case FILE_ERROR:
                return start + " | " + reason;
            default:
                return "DEFAULT";
        }
    }
}


