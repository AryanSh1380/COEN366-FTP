import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class UDPServer {
    static private List<Client> clients = new ArrayList<>();

    public static void main(String[] args) {
        //List<TCPClient> clients = new ArrayList<>();
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(2308);
            while(!socket.isClosed()) {
                byte[] buffer = new byte[1024];

                // Receive the first message
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String message = new String(request.getData(), 0, request.getLength());
                String[] parts = message.split("\\|");

                // parse register request: REGISTER | RQ# | ClientName | IP_Addr | socket#
                String commandType = parts[0].trim();
                Integer REQnumb = Integer.parseInt(parts[1].trim());
                String clientName = parts[2].trim();
                InetAddress clientAddress = request.getAddress();
                Integer clientPort = request.getPort();
                
                String response = handleCommand(commandType, REQnumb, clientName, clientAddress, clientPort);

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

    public static String handleCommand(String commandType, Integer REQnumb, String clientName, InetAddress clientAddress, int clientPort) {
        String response = "";
        switch (commandType) {
            case "REGISTER":
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
                break;
            case "DEREGISTER":
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
                        response = String.format(" De-registering client: %s ", clientToRemove.getClientName());
                        clients.remove(clientToRemove);
                    } else {
                        response = String.format("%s is not registered", clientToRemove.getClientName());
                    }
                }
                break;
            // Add additional cases as needed
            default:
                System.out.println("Unknown command type");
                // Handle unknown command type
                break;
        }
        return response;
    }
}



