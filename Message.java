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
    private String ipAddress;
    private int socket;
    private List<TextFile> files;

    // Client to client
    private int chunk;
    private String text;


    public Message(Type type, int rq) {
        this.type = type;
        this.rq = rq;
    }

    public Message(Type type, int rq, String name) {
        this.type = type;
        this.rq = rq;
        this.name = name;
    }

    public Message(Type type, int rq, Reason reason) {
        this.type = type;
        this.rq = rq;
        this.reason = reason;
    }

    public Message(Type type, int rq, String name, String ipAddress, int socket) {
        this.type = type;
        this.rq = rq;
        this.name = name;
        this.ipAddress = ipAddress;
        this.socket = socket;
    }

    public Message(Type type, int rq, String name, List<TextFile> files) {
        this.type = type;
        this.rq = rq;
        this.name = name;
        this.files = files;
    }

    public Message(Type type, int rq, Reason reason, String name) {
        this.type = type;
        this.rq = rq;
        this.reason = reason;
        this.name = name;
    }

    public Message(Type type, int rq, int socket) {
        this.type = type;
        this.rq = rq;
        this.socket = socket;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getRq() {
        return rq;
    }

    public void setRq(int rq) {
        this.rq = rq;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getSocket() {
        return socket;
    }

    public void setSocket(int socket) {
        this.socket = socket;
    }

    public List<TextFile> getFiles() {
        return files;
    }

    public void setFiles(List<TextFile> files) {
        this.files = files;
    }

    public int getChunk() {
        return chunk;
    }

    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        String start = this.type + " | " + rq;
        switch(this.type) {
            case FILE_REQ:
                return start + " | " + name;
            case FILE_CONF:
                return start + " | " + socket;
            case FILE:
            case FILE_END:
                return start + " | " + name + " | " + chunk + " | " + text;
            case FILE_ERROR:
                return start + " | " + reason;
            default:
                return "DEFAULT";
        }
    }
}

enum Type {
    FILE_REQ,
    FILE_CONF,
    FILE,
    FILE_END,
    FILE_ERROR
}

enum Reason {
    NULL,
    UNKNOWN
}
