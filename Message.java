import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

    // Server to client and client to server
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

    static void send(Socket socket, Message msg) throws IOException {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(msg);
        } catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }

    static Message receive(Socket socket) throws IOException, ClassNotFoundException {
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
    UNKNOWN
}
