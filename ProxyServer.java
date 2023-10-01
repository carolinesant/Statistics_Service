import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer implements ProxyServerInterface {

    class ServerNode {
        String registryName;
        int serverId;
        int waiting = 0;
        int counter = 0;
        ServerNode next;
        // Constructor
        public ServerNode(int serverId) {
            this.serverId = serverId;
        }
    }
    private static final int NUM_SERVERS = 5;
    private final ArrayList<ServerNode> servers = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(NUM_SERVERS);

    /**
     * Constructor of ProxyServer. Makes the 5 servers and binds them in the registry
     */
    public ProxyServer() {    
        
        for (int i = 0; i < NUM_SERVERS; i++) {
            servers.add(new ServerNode(i+1));
        }

        for (int i = 0; i < servers.size(); i++) {
            ServerNode currentServer = servers.get(i);
            if (i == servers.size() - 1) currentServer.next = servers.get(0);
            else currentServer.next = servers.get(i+1);
            try {
                Server obj = new Server();
                ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(obj, 0);
                Registry registry = LocateRegistry.getRegistry();
                registry.rebind("server" + (i+1), stub);
                currentServer.registryName = "server" + (i+1);
                System.err.println("Server " + (i+1) + " is running...");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Figures out which server the client can send the query to, and returns it
     * @param requestedZone The server zone that the client is in
     * @return The registry name of the server that the client will use for the query
     */
    @Override
    public String getServer(int requestedZone) throws RemoteException {
        int requestedZoneIndex = requestedZone - 1;

        ServerNode zoneServer = servers.get(requestedZoneIndex);
        ServerNode neighbor = zoneServer.next;
        ServerNode neighbor2 = neighbor.next;
        int comparison = Integer.compare(neighbor.waiting, neighbor2.waiting);

        ServerNode selectedServer;
       
        if (zoneServer.waiting < 18 || (neighbor.waiting >= 18 && neighbor2.waiting >= 18)) {selectedServer = zoneServer;
        } else if (comparison == 0) {
            Random random = new Random();
            selectedServer = random.nextBoolean() ? neighbor : neighbor2; 
        } else if (comparison < 0) {selectedServer = neighbor;
        } else {selectedServer = neighbor2;}

        selectedServer.counter++;
        if (selectedServer.counter >= 18) {
            selectedServer.counter = 0;
            executor.submit(() -> updateServerInfo(selectedServer));
        }
        return selectedServer.registryName;
    }    

    /**
     * Updates the waiting list number in the ServerNode with the actual number of waiting tasks in the server
     * @param serverToUpdate The server zone that the client is in
     */
    private void updateServerInfo(ServerNode serverToUpdate) {
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface stub = (ServerInterface) registry.lookup(serverToUpdate.registryName);
            serverToUpdate.waiting = stub.getTaskQueueSize();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
    /**
     * Loops through the servers and gets the cache for each one and adds them to a list that is returned
     * @return List of server caches
     */
    @Override
    public List<LinkedHashMap<String, Integer>> getServerCaches() {
        List<LinkedHashMap<String, Integer>> serverCaches = new ArrayList<>();
        for (int i = 0; i < NUM_SERVERS; i++) {
            try {
                ServerNode server = servers.get(i);
                Registry registry = LocateRegistry.getRegistry();
                ServerInterface stub = (ServerInterface) registry.lookup(server.registryName);
                LinkedHashMap<String, Integer> cache = stub.getServerCache();
                serverCaches.add(cache);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
        return serverCaches;
    }

    /**
     * Makes a ProxyServer remote object and binds it in the registry so that the client has access
     */
    public static void main(String[] args) {
        try {
            ProxyServer obj = new ProxyServer();
            ProxyServerInterface stub = (ProxyServerInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("Proxy", stub);
            System.err.println("Proxy is running...");            
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Client.main(args);
    }
}

