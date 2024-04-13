import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Client implements Serializable {
    private String clientName;
    private InetAddress clientAddress;
    private Integer clientPort;
    private List<String> listOfPublishedFiles;
    
    public Client(String clientName, InetAddress clientAddress, Integer clientPort) {
        listOfPublishedFiles = new ArrayList<>();
        this.clientName = clientName;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    public void addFileToList(String fileName) {
        listOfPublishedFiles.add(fileName);
    }

    public void removeFileFromList(String fileName) {
        listOfPublishedFiles.remove(fileName);
    }

    public void printFileNameFromList() {
        for(String filename : listOfPublishedFiles) {
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

    @Override
    public String toString() {
        String files = listOfPublishedFiles.toString();
        return clientName + "@" + clientAddress + "::" + clientPort + " " + files;
    }
}