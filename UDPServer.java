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
    static private Message response;
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

                InetAddress clientAddress = request.getAddress();
                Integer clientPort = request.getPort();


                switch (msg.getType()) {
                    case REGISTER:
                        response = handleRegisterCommand(msg);
                        response.send(clientAddress,clientPort,socket);
                        break;
                    case DEREGISTER:
                        handleDeregisterCommand(msg);
                        break;
                    case PUBLISH:
                        response = handlePublishCommand(msg);
                        response.send(clientAddress,clientPort,socket);
                        break;
                    case REMOVE:
                        response = handleRemoveCommand(msg);
                        response.send(clientAddress,clientPort,socket);
                        break;
                    default:
                        System.out.println("Unknown command type");
                        // Handle unknown command type
                        break;
                }
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

    private static Message handleRemoveCommand(Message msg) {
        Message response = null;
        int REQnumb = msg.getRq();
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
                    response = new Message(Type.REMOVED, REQnumb);
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
                response = new Message(Type.REMOVE_DENIED, REQnumb, Reason.CLIENT_NAME_DNE);
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

    private static Message handlePublishCommand(Message msg) {
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
                    response = new Message(Type.PUBLISHED, REQnumb);
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
                response = new Message(Type.PUBLISH_DENIED, REQnumb, Reason.CLIENT_NAME_DNE);
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

    private static void handleDeregisterCommand(Message msg) {
        int REQnumb = msg.getRq();
        String clientName = msg.getName();
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
                response = new Message(Type.DEREGISTERED, clientToRemove.getClientName(), REQnumb);
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
                response = new Message(Type.NOT_REGISTERED, clientToRemove.getClientName(), REQnumb);
            }
        } else {
            System.out.println("Client list is empty.");
        }
        System.out.println("List of registered clients:");
        for (Client client : clients) {
            System.out.println(client.getClientName());
        }
        System.out.println("----------------------------------------------------------------------");
        System.out.println(response);
    }

    private static Message handleRegisterCommand(Message msg) {
        int REQnumb = msg.getRq();
        String clientName = msg.getName();
        InetAddress clientAddress = msg.getIpAddress();
        int clientPort = msg.getRq();
        boolean clientNameExists = false;
        if(clients.isEmpty()) {
            Client newClient = new Client(clientName, clientAddress, clientPort);
            clients.add(newClient);
            response = new Message(Type.REGISTERED, REQnumb);
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
                response = new Message(Type.REGISTERED, REQnumb);
                ServerUpdate = true;
            } else {
                response = new Message(Type.REGISTER_DENIED, REQnumb, Reason.CLIENT_NAME_ALREADY_IN_USE);
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