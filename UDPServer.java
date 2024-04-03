import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UDPServer {
    static private List<Client> clients = new ArrayList<>();
    static private List<String> filenamesToRemove = new ArrayList<>();
    static private List<String> filenamesToPublish = new ArrayList<>();
    static private boolean ServerUpdate = false;
    static private DatagramSocket socket;
    static private Timer timer;
    static private long timerPeriod = 10000;
    public static void main(String[] args) {
        try {
            socket = new DatagramSocket(2308);
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UDPServer.updateAllClients(socket);
                }
            }, 0, timerPeriod);
            while(!socket.isClosed()) {
                byte[] buffer = new byte[1024];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                byte[] receivedData = request.getData();
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivedData));
                Message msg = (Message) in.readObject();
               
                // String message = new String(request.getData(), 0, request.getLength());
                InetAddress clientAddress = request.getAddress();
                Integer clientPort = request.getPort();

                // String[] parts = message.split("\\|");
                // String commandType = parts[0].trim();
                String response = "";

                switch (msg.getType()) {
                    case REGISTER:
                        response = handleRegisterCommand(msg);
                        break;
                    case DEREGISTER:
                        response = handleDeregisterCommand(msg);
                        break;
                    case PUBLISH:
                        response = handlePublishCommand(msg);
                        break;
                    case REMOVE:
                        response = handleRemoveCommand(msg);
                        break;
                    default:
                        System.out.println("Unknown command type");
                        // Handle unknown command type
                        break;
                }

                byte[] sendData = response.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                socket.send(reply);
                if(ServerUpdate) {
                    updateAllClients(socket);
                    ServerUpdate = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }   
    }

    private static void updateAllClients(DatagramSocket socket) {
        try {
            //UPDATE List of (Name, IP address, UDP socket#, list of available files)
            for(Client client : clients) {
                String updateMsg = "UPDATE | List of (Name, IP address, UDP socket#, list of available files)";
                // byte[] sendData = updateMsg.getBytes();
                // DatagramPacket reply = new DatagramPacket(sendData, sendData.length, client.getClientAddress(), client.getClientPort());
                // socket.send(reply);
                System.out.println(updateMsg);
            }
        } catch (Exception e) {
            e.printStackTrace(); 
        }
    }

    private static String handleRemoveCommand(Message msg) {
        String response = "";
        //Integer REQnumb = Integer.parseInt(parts[1].trim());
        int REQnumb = msg.getRq();
        //String clientName = parts[2].trim();
        String clientName = msg.getName();
        filenamesToRemove = msg.getFiles();
        boolean clientNameDNE = true;
        if(!clients.isEmpty()){
            for (Client client : clients) {
                String name = client.getClientName();
                if (name != null && name.equals(clientName)) { 
                    for (String fileToRemove : filenamesToRemove) {
                        client.removeFileFromList(fileToRemove);   
                    }
                    response = String.format("REMOVED | %d", REQnumb);
                    ServerUpdate = true;
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            UDPServer.updateAllClients(socket);
                        }
                    }, 0, timerPeriod);
                    clientNameDNE = false;
                    break;
                }
            }
            if (clientNameDNE) {
                response = String.format("REMOVE-DENIED | %d | Client Name does not exist",  REQnumb);
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

    private static String handlePublishCommand(Message msg) {
        String response = "";
        // Integer REQnumb = Integer.parseInt(parts[1].trim());
        // String clientName = parts[2].trim();
        int REQnumb = msg.getRq();
        String clientName = msg.getName();
        filenamesToPublish = msg.getFiles();
        boolean clientNameDNE = true;
        if(!clients.isEmpty()){
            for (Client client : clients) {
                String name = client.getClientName();
                if (name != null && name.equals(clientName)) { 
                    for (String fileToAdd : filenamesToPublish) {
                        client.addFileToList(fileToAdd);   
                    }
                    response = String.format("PUBLISHED | %d", REQnumb);
                    ServerUpdate = true;
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            UDPServer.updateAllClients(socket);
                        }
                    }, 0, timerPeriod);
                    clientNameDNE = false;
                    break;
                }
            }
            if (clientNameDNE) {
                response = String.format("PUBLISH-DENIED | %d | Client Name does not exist",  REQnumb);
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

    private static String handleDeregisterCommand(Message msg) {
        int REQnumb = msg.getRq();
        String clientName = msg.getName();
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
                ServerUpdate = true;
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        UDPServer.updateAllClients(socket);
                    }
                }, 0, timerPeriod);
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

    private static String handleRegisterCommand(Message msg) {
        int REQnumb = msg.getRq();
        String clientName = msg.getName();
        InetAddress clientAddress = msg.getIpAddress();
        int clientPort = msg.getRq();
        String response = "";
        boolean clientNameExists = false;
        if(clients.isEmpty()) {
            Client newClient = new Client(clientName, clientAddress, clientPort);
            clients.add(newClient);
            response = String.format("REGISTERED | %d", REQnumb);
            ServerUpdate = true;
            timer.cancel();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UDPServer.updateAllClients(socket);
                }
            }, 0, timerPeriod);
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
                ServerUpdate = true;
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