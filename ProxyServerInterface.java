import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.List;

public interface ProxyServerInterface extends Remote {
    String getServer(int zoneNumber) throws RemoteException;
    List<LinkedHashMap<String, Integer>> getServerCaches() throws RemoteException;
}