import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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


    public Boolean isPublished(String f) {
        Boolean isPublished = false;
        for(String filename : listOfPublishedFiles) {
            if(filename.equals(f)) isPublished = true;
        }
        return isPublished;
    }

    public void updateClientPort(Integer port) {
        clientPort = port;

    }

    public void updateClientAddress(InetAddress address) {
        clientAddress = address;

    }

    @Override
    public String toString() {
        String files = listOfPublishedFiles.toString();
        return clientName + "@" + clientAddress + ":" + clientPort + " " + files;
    }
}