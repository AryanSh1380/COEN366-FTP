import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private String clientName;
    private InetAddress clientAddress;
    private Integer clientPort;
    private List<String> listOfFiles;

    public Client(String clientName, InetAddress clientAddress, Integer clientPort) {
        listOfFiles = new ArrayList<>();
        this.clientName = clientName;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    public void addFileToList(String fileName) {
        listOfFiles.add(fileName);
    }

    public void removeFileFromList(String fileName) {
        listOfFiles.remove(fileName);
    }

    public void printFileNameFromList() {
        for (String filename : listOfFiles) {
            System.out.println(filename);
        }
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

    public void updateClientAddress(InetAddress address) {
        clientAddress = address;

    }

    public void updateClientPort(Integer port) {
        clientPort = port;

    }
}