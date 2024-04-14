import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class UDPServer {
    static private List<Client> clients = new ArrayList<>();
    static private boolean ServerUpdate = false;
    static private DatagramSocket socket;
    static private Timer timer;
    static private Semaphore s = new Semaphore(1);
    static private int serverPort = 2308;
    static private long timerPeriod = 10000;
    //static private Message response;
    public static void main(String[] args) {
        try {
            socket = new DatagramSocket(serverPort);
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UDPServer.updateAllClients(socket);
                }
            }, 0, timerPeriod);
            while(!socket.isClosed()) {
                System.out.println("Server is running at: " + InetAddress.getLocalHost().toString() + ":" + serverPort);
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
                        Thread register = new Thread(new RegisterThread(msg, clientAddress,clientPort));
                        register.start();
                        break;
                    case DEREGISTER:
                        Thread deregister = new Thread(new DeregisterThread(msg, clientAddress,clientPort));
                        deregister.start();
                        // handleDeregisterCommand(msg);
                        break;
                    case PUBLISH:
                        Thread publish = new Thread(new PublishThread(msg, clientAddress,clientPort));
                        publish.start();
                        // response = handlePublishCommand(msg);
                        // response.send(clientAddress,clientPort,socket);
                        break;
                    case REMOVE:
                        Thread remove = new Thread(new RemoveThread(msg, clientAddress,clientPort));
                        remove.start();
                        // response = handleRemoveCommand(msg);
                        // response.send(clientAddress,clientPort,socket);
                        break;
                    case UPDATE:
                        Thread update = new Thread(new UpdateThread(msg, clientAddress,clientPort));
                        update.start();
                        // response = handleUpdateCommand(msg);
                        // response.send(clientAddress,clientPort,socket);
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

    public static void updateAllClients(DatagramSocket socket) {
            try {
                s.acquire();
                if(!clients.isEmpty()) {
                    Message updateClient = new Message(Type.UPDATE, clients);
                    for (Client client : clients) {
                        updateClient.send(client.getClientAddress(), client.getClientPort(), socket);
                    }
                    System.out.println(updateClient.toString());
                }
            } catch(Exception e){
                e.printStackTrace();
            } finally {
              s.release();
            }
    }

    static class UpdateThread implements Runnable {
        private Message response;
        private Integer REQnumb;
        private String clientName;
        private InetAddress updatedclientAddress;
        private Integer updatedclientPort;
        private InetAddress destinationAddress;
        private Integer destinationPort;

        UpdateThread(Message msg, InetAddress destinationAddress, Integer destinationPort) {
            REQnumb = msg.getRq();
            clientName = msg.getName();
            updatedclientAddress = msg.getIpAddress();
            updatedclientPort = msg.getSocketNum();
            this.destinationAddress = destinationAddress;
            this.destinationPort = destinationPort;
        }

        @Override
        public void run() {
            try {
                if (!clients.isEmpty()) {
                    boolean ipAddressAndPortUsed = false;

                    for (Client client : clients) {
                        if (updatedclientAddress.equals(client.getClientAddress()) && updatedclientPort.equals(client.getClientPort())) {
                            ipAddressAndPortUsed = true;
                            break;
                        }
                    }

                    if (ipAddressAndPortUsed) {
                        System.out.println("IP Address and UDP port are currently being used together. Cannot update.");
                        response = new Message(Type.UPDATE_DENIED, REQnumb, clientName, Reason.IP_ADDRESS_AND_PORT_NOT_CHANGED);
                    } else {
                        for (Client client : clients) {
                            if (clientName.equals(client.getClientName())) {
                                if (updatedclientAddress.equals(client.getClientAddress())) {
                                    client.updateClientPort(updatedclientPort);
                                    System.out.println("IP Address remains the same, UDP Port is being updated...");
                                    response = new Message(Type.UPDATE_CONFIRMED, REQnumb, clientName, updatedclientAddress, updatedclientPort);
                                } else {
                                    client.updateClientAddress(updatedclientAddress);
                                    client.updateClientPort(updatedclientPort);
                                    response = new Message(Type.UPDATE_CONFIRMED, REQnumb, clientName, updatedclientAddress, updatedclientPort);
                                }
                                timer.cancel();
                                timer = new Timer();
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        UDPServer.updateAllClients(socket);
                                    }
                                }, 0, timerPeriod);
                                break; // Exit the loop once the client is found and updated
                            }
                        }
                    }
                } else {
                    System.out.println("Client list is empty.");
                }
                response.send(destinationAddress, destinationPort, socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class RemoveThread implements Runnable {
        private List<String> filenamesToRemove;
        private Message response;
        private int REQnumb;
        private String clientName;
        private boolean clientNameDNE;
        private InetAddress destinationAddress;
        private Integer destinationPort;

        RemoveThread(Message msg, InetAddress destinationAddress, Integer destinationPort) {
            filenamesToRemove = new ArrayList<>();
            REQnumb = msg.getRq();
            clientName = msg.getName();
            filenamesToRemove = msg.getFiles();
            clientNameDNE = true;
            this.destinationAddress = destinationAddress;
            this.destinationPort = destinationPort;
        }

        @Override
        public void run() {
            try{
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
                response.send(destinationAddress, destinationPort, socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class PublishThread implements Runnable {
        private List<String> filenamesToPublish;
        private Message response;
        private int REQnumb;
        private String clientName;
        private boolean clientNameDNE;
        private InetAddress destinationAddress;
        private Integer destinationPort;

        PublishThread(Message msg, InetAddress destinationAddress, Integer destinationPort) {
            filenamesToPublish = new ArrayList<>();
            REQnumb = msg.getRq();
            filenamesToPublish = msg.getFiles();
            clientName = msg.getName();
            clientNameDNE = true;
            this.destinationAddress = destinationAddress;
            this.destinationPort = destinationPort;
        }

        // Wait for file over provided TCP socket number
        @Override
        public void run() {
            try {
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
                response.send(destinationAddress, destinationPort, socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class DeregisterThread implements Runnable {
        private Message response;
        private int REQnumb;
        private String clientName;
        private boolean clientNameDNE;
        private InetAddress destinationAddress;
        private Integer destinationPort;
        private Client clientToRemove = null;

        DeregisterThread(Message msg, InetAddress destinationAddress, Integer destinationPort) {
            REQnumb = msg.getRq();
            clientName = msg.getName();
            clientNameDNE = true;
            this.destinationAddress = destinationAddress;
            this.destinationPort = destinationPort;
        }

        @Override
        public void run() {
            try {
                s.acquire();
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
                response.send(destinationAddress, destinationPort, socket);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                s.release();
            }
        }
    }

    static class RegisterThread implements Runnable {
        private Message response;
        private int REQnumb;
        private String clientName;
        private InetAddress clientAddress;
        private int clientPort;
        private InetAddress destinationAddress;
        private Integer destinationPort;
        private boolean clientNameExists = false;

        RegisterThread(Message msg, InetAddress destinationAddress, Integer destinationPort) {
            REQnumb = msg.getRq();
            clientName = msg.getName();
            clientAddress = msg.getIpAddress();
            clientPort = msg.getSocketNum();
            clientNameExists = false;
            this.destinationAddress = destinationAddress;
            this.destinationPort = destinationPort;
        }

        @Override
        public void run() {
            try {
                s.acquire();
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
                response.send(destinationAddress, destinationPort, socket);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                s.release();
            }
        }
    }
}