import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Queue;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** 
 * Server 
 * The ServerInterface exposes the following methods that have to be implemented by a servant class.
 * */

public class Server implements ServerInterface {

    private static final int MAX_CACHE_SIZE = 150; // Specify the maximum cache size
    LinkedHashMap<String, Integer> cache = new LinkedHashMap<String, Integer>(MAX_CACHE_SIZE + 1, 1.0f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private Queue<Runnable> taskQueue = new LinkedList<>(); // Kø for oppgaver som behandles asynkront.

    private final String dataSetFile = "exercise_1_dataset.csv";

    // Forbedret konstruktør med oppstart av oppgaveutførelsestråd.
    public Server() throws RemoteException {

        // Starter en egen tråd for å håndtere asynkron oppgaveutførelse.
        new Thread(() -> {
            while (true) {
                Runnable task;
                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                            // Venter på oppgaver hvis køen er tom.
                            taskQueue.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    task = taskQueue.poll();
                }
                task.run(); // Utfører oppgaven asynkront.
            }
        }).start();
    }
    
    /**
     * Returns the server cache.
     *
     * @return the server cache
     */
    @Override
    public LinkedHashMap<String, Integer> getServerCache() {
        LinkedHashMap<String, Integer> serverCache = new LinkedHashMap<String, Integer>();
        synchronized (this.cache) {
            serverCache.putAll(this.cache);
        }
        return serverCache; 
    } 

     /**
     * Returns the number of tasks in the task queue.
     *
     * @return the size of the task queue
     */
    @Override
    public int getTaskQueueSize() {
        synchronized (taskQueue) {
            return taskQueue.size();
        }
    }

    /**
     * Returns the population of the given country. The cache is checked 
     * for the result before the data set is searched. The server wraps the 
     * task in a FutureTask and adds it to a synchronized queue.
     * The server then notifies the worker thread that there is a new task in the queue.
     * The server then waits for the worker thread to complete the task and return the result.
     * 
     * @param countryName the name of the country to retrieve the population for
     * @return an array of three long values representing the population, the execution time, and the waiting time
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public long[] getPopulationofCountry(String countryName) throws RemoteException {
        long waitingStartTime = System.currentTimeMillis();
        
        FutureTask<long[]> futureTask = new FutureTask<>(() -> {
            long waitingStopTime = System.currentTimeMillis();
            long waitingTime = (waitingStopTime - waitingStartTime);
            long executionStartTime = System.currentTimeMillis();
            simulateLatency(80);
            String cacheKey = "getPopulationofCountry_" + countryName;
            synchronized (cache) {
                if (cache.containsKey(cacheKey)) {
                    long executionEndTime = System.currentTimeMillis();
                    long executionTime = (executionEndTime - executionStartTime);
                    return new long[] {cache.get(cacheKey), executionTime, waitingTime};
                }
            }
            int populationSize = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(dataSetFile))) {
                String line;
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(";");
                    String country = data[3].trim();
                    if (country.equals(countryName)) {
                        populationSize += Integer.parseInt(data[4]);
                    }
                } 
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (cache) {
                cache.put(cacheKey, populationSize);
            }
            long executionEndTime = System.currentTimeMillis();
            long executionTime = (executionEndTime - executionStartTime);
            return new long[] {populationSize, executionTime, waitingTime};
        });

        synchronized (taskQueue) {
            taskQueue.add(futureTask);
            taskQueue.notify();
        }

        try {
            return futureTask.get();  // This will block until the FutureTask completes
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Task execution failed", e);
        }
    }

    /**
     * Returns the number of cities in the given country with a population greater than or equal to the given minimum.
     * The cache is checked for the result before the data set is searched. The server wraps the
     * task in a FutureTask and adds it to a synchronized queue.
     * The server then notifies the worker thread that there is a new task in the queue.
     * The server then waits for the worker thread to complete the task and return the result.
     * 
     * @param countryName the name of the country to retrieve the number of cities for
     * @param min the minimum population size of the cities
     * @return an array of three long values representing the number of cities, the execution time, and the waiting time
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public long[] getNumberofCities(String countryName, int min) throws RemoteException {
        long waitingStartTime = System.currentTimeMillis();
        FutureTask<long[]> futureTask = new FutureTask<>(() -> {
            long waitingStopTime = System.currentTimeMillis();
            long waitingTime = (waitingStopTime - waitingStartTime);
            long executionStartTime = System.currentTimeMillis();
            simulateLatency(80);
            String cacheKey = "getNumberOfCities_" + countryName + "_" + min;
            synchronized (cache) {
                if (cache.containsKey(cacheKey)) {
                    long executionEndTime = System.currentTimeMillis();
                    long executionTime = (executionEndTime - executionStartTime);
                    return new long[] {cache.get(cacheKey), executionTime, waitingTime};
                }
            }
            int cityCount = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(dataSetFile))) {
                String line;
                reader.readLine(); 
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(";");
                    if (data[3].trim().equals(countryName) && Integer.parseInt(data[4].trim()) >= min) {
                        cityCount++;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (cache) {
                cache.put(cacheKey, cityCount);
            }
            long executionEndTime = System.currentTimeMillis();
            long executionTime = (executionEndTime - executionStartTime);
            return new long[] {cityCount, executionTime, waitingTime};
        });
    
        synchronized (taskQueue) {
            taskQueue.add(futureTask);
            taskQueue.notify();
        }
    
        try {
            return futureTask.get();  // This will block until the FutureTask completes
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Task execution failed", e);
        }

    }

    /**
     * Returns the number of countries with at least the given number of cities
     * and a population greater than or equal to the given minimum.
     * The cache is checked for the result before the data set is searched. The server wraps the
     * task in a FutureTask and adds it to a synchronized queue.
     * The server then notifies the worker thread that there is a new task in the queue.
     * The server then waits for the worker thread to complete the task and return the result.
     * 
     * @param citycount the minimum number of cities in a country
     * @param minpopulation the minimum population size of the cities
     * @return an array of three long values representing the number of countries, the execution time, and the waiting time
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public long[] getNumberofCountries(int citycount, int minpopulation) throws RemoteException {
        long waitingStartTime = System.currentTimeMillis();
        FutureTask<long[]> futureTask = new FutureTask<>(() -> {
            long waitingStopTime = System.currentTimeMillis();
            long waitingTime = (waitingStopTime - waitingStartTime);
            long executionStartTime = System.currentTimeMillis();
            simulateLatency(80);
            String cacheKey = "getNumberOfCountries_" + citycount + "_" + minpopulation;
            synchronized (cache) {
                if (cache.containsKey(cacheKey)) {
                    long executionEndTime = System.currentTimeMillis();
                    long executionTime = (executionEndTime - executionStartTime);
                    return new long[] {cache.get(cacheKey), executionTime, waitingTime};
                }
            }
            Map<String, Integer> countryCityCounts = new HashMap<>(); // Map to hold counts of cities for each country
            try (BufferedReader reader = new BufferedReader(new FileReader(dataSetFile))) {
                String line;
                // Skip the header
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    // Check if the 'Population' is greater than or equal to minPopulation
                    if (Integer.parseInt(parts[4].trim()) >= minpopulation) {
                        countryCityCounts.put(parts[3].trim(), countryCityCounts.getOrDefault(parts[3].trim(), 0) + 1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Now, filter out the countries with cities count greater than or equal to cityCount
            int result = (int) countryCityCounts.values().stream().filter(count -> count >= citycount).count();
            synchronized (cache) {
                cache.put(cacheKey, result);
            }
            long executionEndTime = System.currentTimeMillis();
            long executionTime = (executionEndTime - executionStartTime);
            return new long[] {result, executionTime, waitingTime};
        });

        synchronized (taskQueue) {
            taskQueue.add(futureTask);
            taskQueue.notify();
        }

        try {
            return futureTask.get();  // This will block until the FutureTask completes
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Task execution failed", e);
        }
    }

    /**
     * Returns the number of countries with at least the given number of cities
     * and a population greater than or equal to the given minimum and less than or equal to the given maximum.
     * The cache is checked for the result before the data set is searched. The server wraps the
     * task in a FutureTask and adds it to a synchronized queue.
     * The server then notifies the worker thread that there is a new task in the queue.
     * The server then waits for the worker thread to complete the task and return the result.
     * 
     * @param citycount the minimum number of cities in a country
     * @param minpopulation the minimum population size of the cities
     * @param maxpopulation the maximum population size of the cities
     * @return an array of three long values representing the number of countries, the execution time, and the waiting time
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public long[] getNumberofCountries(int citycount, int minpopulation, int maxpopulation) throws RemoteException {
        long waitingStartTime = System.currentTimeMillis();
        FutureTask<long[]> futureTask = new FutureTask<>(() -> {
            long waitingStopTime = System.currentTimeMillis();
            long waitingTime = (waitingStopTime - waitingStartTime);
            long executionStartTime = System.currentTimeMillis();
            simulateLatency(80);
            String cacheKey = "getNumberOfCountries_" + citycount + "_" + minpopulation + "_" + maxpopulation;
            synchronized (cache) {
                if (cache.containsKey(cacheKey)) {
                    long executionEndTime = System.currentTimeMillis();
                    long executionTime = (executionEndTime - executionStartTime);
                    return new long[] {cache.get(cacheKey), executionTime, waitingTime};
                }
            }
            Map<String, Integer> countryCityCounts = new HashMap<>(); // Map to hold counts of cities for each country
            try (BufferedReader reader = new BufferedReader(new FileReader(dataSetFile))) {
                String line;
                // Skip the header
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    // Check if the 'Population' is greater than or equal to minPopulation
                    int cityPopulation = Integer.parseInt(parts[4].trim());
                    if (cityPopulation >= minpopulation && cityPopulation <= maxpopulation) {
                        countryCityCounts.put(parts[3].trim(), countryCityCounts.getOrDefault(parts[3].trim(), 0) + 1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            int result = (int) countryCityCounts.values().stream().filter(count -> count >= citycount).count();
            
            synchronized (cache) {
                cache.put(cacheKey, result);
            }
            long executionEndTime = System.currentTimeMillis();
            long executionTime = (executionEndTime - executionStartTime);
            return new long[] {result, executionTime, waitingTime};
        });

        synchronized (taskQueue) {
            taskQueue.add(futureTask);
            taskQueue.notify();
        }

        try {
            return futureTask.get();  // This will block until the FutureTask completes
        } catch (InterruptedException | ExecutionException e) {
            throw new RemoteException("Task execution failed", e);
        }
    }

    /*
    * Simulates latency by sleeping for the specified number of milliseconds.
    */
    private void simulateLatency(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        try {
            Server obj = new Server();
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("ServerInterface", stub);

            System.err.println("Server is running...");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    




}
