import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class UDPServer {
    static private List<Client> clients = new ArrayList<>();

    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(2308);
            while(!socket.isClosed()) {
                byte[] buffer = new byte[1024];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String message = new String(request.getData(), 0, request.getLength());
                InetAddress clientAddress = request.getAddress();
                Integer clientPort = request.getPort();

                String[] parts = message.split("\\|");
                String commandType = parts[0].trim();
                String response = "";

                switch (commandType) {
                    case "REGISTER":
                        response = handleRegisterCommand(parts, clientAddress, clientPort);
                        break;
                    case "DEREGISTER":
                        response = handleDeregisterCommand(parts);
                        break;
                    case "PUBLISH":
                        response = handlePublishCommand(parts);
                        break;
                    case "REMOVE":
                        response = handleRemoveCommand(parts);
                        break;
                    default:
                        System.out.println("Unknown command type");
                        // Handle unknown command type
                        break;
                }

                byte[] sendData = response.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                socket.send(reply);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }   
    }

    private static String handleRemoveCommand(String[] parts) {
        String response = "";
        Integer REQnumb = Integer.parseInt(parts[1].trim());
        String clientName = parts[2].trim();
        boolean clientNameDNE = true;
        if(!clients.isEmpty()){
            for (Client client : clients) {
                String name = client.getClientName();
                if (name != null && name.equals(clientName)) { 
                    for (int i = 3; i < parts.length; i++) {
                        client.removeFileFromList(parts[i]);   
                    }
                    response = String.format("REMOVED | %s", REQnumb);
                    clientNameDNE = false;
                    break;
                }
            }
            if (clientNameDNE) {
                response = String.format("REMOVE-DENIED | %s | Client Name does not exist",  REQnumb);
            }
        } else {
            System.out.println("Client list is empty.");
        }
        System.out.println("Files available for share:");
        for (Client client : clients) {
            client.printFileNameFromList();
        }
        System.out.println("----------------------------------------------------------------------");
        return response;
    }

    private static String handlePublishCommand(String[] parts) {
        String response = "";
        Integer REQnumb = Integer.parseInt(parts[1].trim());
        String clientName = parts[2].trim();
        boolean clientNameDNE = true;
        if(!clients.isEmpty()){
            for (Client client : clients) {
                String name = client.getClientName();
                if (name != null && name.equals(clientName)) { 
                    for (int i = 3; i < parts.length; i++) {
                        client.addFileToList(parts[i]);   
                    }
                    response = String.format("PUBLISHED | %s", REQnumb);
                    clientNameDNE = false;
                    break;
                }
            }
            if (clientNameDNE) {
                response = String.format("PUBLISH-DENIED | %s | Client Name does not exist",  REQnumb);
            }
        } else {
            System.out.println("Client list is empty.");
        }
        System.out.println("Files available for share:");
        for (Client client : clients) {
            client.printFileNameFromList();
        }
        System.out.println("----------------------------------------------------------------------");
        return response;
    }

    private static String handleDeregisterCommand(String[] parts) {
        Integer REQnumb = Integer.parseInt(parts[1].trim());
        String clientName = parts[2].trim();
        String response = "";
        boolean clientNameDNE = true;
        Client clientToRemove = null;
        if(!clients.isEmpty()) {
            for (Client client : clients) {
                String name = client.getClientName();
                if (name != null && name.equals(clientName)) { 
                    clientToRemove = client;
                    clientNameDNE = false;
                    break;
                }
            }
            if (!clientNameDNE) {
                response = String.format("De-registering client: %s | %d", clientToRemove.getClientName(), REQnumb);
                clients.remove(clientToRemove);
            } else {
                response = String.format("%s is not registered | %d", clientToRemove.getClientName(), REQnumb);
            }
        } else {
            System.out.println("Client list is empty.");
        }
        System.out.println("List of registered clients:");
        for (Client client : clients) {
            System.out.println(client.getClientName());
        }
        System.out.println("----------------------------------------------------------------------");
        return response;
    }

    private static String handleRegisterCommand(String[] parts,InetAddress clientAddress, Integer clientPort) {
        Integer REQnumb = Integer.parseInt(parts[1].trim());
        String clientName = parts[2].trim();
        String response = "";
        boolean clientNameExists = false;
        if(clients.isEmpty()) {
            Client newClient = new Client(clientName, clientAddress, clientPort);
            clients.add(newClient);
            response = String.format("REGISTERED | %s", REQnumb);
        } else {
            for (Client client : clients) {
                String name = client.getClientName();
                if (name != null && name.equals(clientName)) { //register denied
                    clientNameExists = true;
                    break;
                }
            }
            if (!clientNameExists) {
                Client newClient = new Client(clientName, clientAddress, clientPort);
                clients.add(newClient);
                response = String.format("REGISTERED | %s", REQnumb);
            } else {
                response = String.format("REGISTER-DENIED | %s | Name already taken by another Client",  REQnumb);
            }
        }
        System.out.println("List of registered clients:");
        for (Client client : clients) {
            System.out.println(client.getClientName());
        }
        System.out.println("----------------------------------------------------------------------");
        return response;
    }
}