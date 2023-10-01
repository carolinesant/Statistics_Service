import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;

// Definerer et Java-grensesnitt kalt StatisticsInterface som utvider Remote-grensesnittet.
public interface ServerInterface extends Remote {
    int getTaskQueueSize() throws RemoteException;
    long[] getPopulationofCountry(String countryName) throws RemoteException; // Metode for å hente befolkningen til et land basert på landets navn.
    long[] getNumberofCities(String countryName, int min) throws RemoteException;     // Metode for å hente antallet byer i et land som har minst et gitt antall innbyggere.
    long[] getNumberofCountries(int citycount, int minpopulation) throws RemoteException; // Metode for å hente antallet land som har minst et visst antall byer og minst en viss befolkning.
    long[] getNumberofCountries(int citycount, int minpopulation, int maxpopulation) throws RemoteException; // Metode for å hente antallet land som har minst et visst antall byer og en befolkning innenfor et gitt område.
    LinkedHashMap<String, Integer> getServerCache() throws RemoteException;
}
