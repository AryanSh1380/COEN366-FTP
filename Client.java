import java.net.InetAddress;

public class Client {
    private String clientName;
    private InetAddress clientAddress;
    private Integer clientPort;
    
    public Client(String clientName, InetAddress clientAddress, Integer clientPort) {
        this.clientName = clientName;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    public String getClientName() {
        return clientName;
    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public Integer getClientPort() {
        return clientPort;
    }
}
