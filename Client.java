import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Client {

    private Client() {}
    private static final int MAX_CACHE_SIZE = 45; // Specify the maximum cache size
    LinkedHashMap<String, Integer> cache = new LinkedHashMap<String, Integer>(MAX_CACHE_SIZE + 1, 1.0f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    /**
     * Goes through the input file and for each query creates a new thread that calls on invokeRemoteMethods
     * @param filename filename of the file with the queries
     */
    private void parseInputFile(String filename) {
        List<Thread> threads = new ArrayList<>();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            System.err.println("File not found");
        }
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            boolean inCache = checkCache(line);
            if (inCache) continue;
            String[] tokens = line.split(" ");
            String methodName = tokens[0];
            Runnable task = new Runnable() {
                public void run() {
                    invokeRemoteMethods(methodName, tokens);
                }
            };
            Thread thread = new Thread(task);
            thread.start();
            threads.add(thread);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        scanner.close();
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if the query is in cache
     * 
     * @param query the query to check
     * @return true if the query is in cache, false otherwise
     */
    private boolean checkCache(String query) {
        if (cache.containsKey(query)) {
            int result = cache.get(query);
            addResultToFile(result, query.split(" "), 0, 0, 0, "cache");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the cache with the query and result
     * @param query the query to add to the cache
     * @param result the result of the query
     */
    private synchronized void updateCache(String query, int result) {
            cache.put(query, result);
    }
    
    /**
     * Invokes the correct method with args based on the method name
     * @param methodName
     * @param args
     */
    private void invokeRemoteMethods(String methodName, String[] args) {
        switch(methodName) {
            case "getPopulationofCountry":
                invokeGetPopulationOfCountry(args);
                break;
            case "getNumberofCities":
                invokeGetNumberOfCities(args);
                break;
            case "getNumberofCountries":
                invokeGetNumberOfCountries(args);
                break;
            default:
                System.out.println("Invalid method name" + methodName);
        }
    }

    /**
     * Invokes the getPopulationOfCountry method on the server.
     * Updates the cache and writes the result to the output file.
     * 
     * @param args the arguments to the method
     */
    private void invokeGetPopulationOfCountry(String[] args) {
        try {
            int argsLen = args.length;
            int zone = (args[argsLen-1].charAt(5)) - '0';
            Registry registry = LocateRegistry.getRegistry();
            ProxyServerInterface proxyStub = (ProxyServerInterface) registry.lookup("Proxy");
            String serverName = proxyStub.getServer(zone);

            // connect to server
            ServerInterface stub = (ServerInterface) registry.lookup(serverName);
           
            int countryLen = argsLen - 2;
            String country = args[1];
            for (int i = 1; i < countryLen; i++) country = country + " " + args[i + 1] ;
            long startTime = System.currentTimeMillis();
            long[] result = stub.getPopulationofCountry(country);
            if (zone != Integer.parseInt(serverName.substring(6))) simulateLatency(90);
            long endTime = System.currentTimeMillis();
            long turnAroundTime = endTime - startTime;
            updateCache(String.join(" ", args), (int)result[0]);
            addResultToFile((int)result[0], args, turnAroundTime, result[1], result[2], serverName);
            System.out.println(String.join(" ", args) + ": " + result[0] + " --turnaroundtime: " + turnAroundTime + " executiontime: " + result[1] + " waitingtime: " + result[2]);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Invokes the getNumberOfCities method on the server.
     * Updates the cache and writes the result to the output file.
     * 
     * @param args the arguments to the method
     */
    private void invokeGetNumberOfCities(String[] args) {
        try {
            int argsLen = args.length;
            int zone = (args[argsLen-1].charAt(5)) - '0';
            Registry registry = LocateRegistry.getRegistry();
            ProxyServerInterface proxyStub = (ProxyServerInterface) registry.lookup("Proxy");
            String serverName = proxyStub.getServer(zone);

            // connect to server
            ServerInterface stub = (ServerInterface) registry.lookup(serverName);

            int countryLen = argsLen - 3;
            String country = args[1];
            for (int i = 1; i < countryLen; i++) country = country + " " + args[i + 1] ;
            long startTime = System.currentTimeMillis();
            long[] result = stub.getNumberofCities(country, Integer.parseInt(args[argsLen - 2]));
            if (zone != Integer.parseInt(serverName.substring(6))) simulateLatency(90);
            long endTime = System.currentTimeMillis();
            long turnAroundTime = endTime - startTime;
            updateCache(String.join(" ", args), (int)result[0]);
            addResultToFile((int)result[0], args, turnAroundTime, result[1], result[2], serverName);
            System.out.println(String.join(" ", args) + ": " + result[0] + " --turnaroundtime: " + turnAroundTime + " executiontime: " + result[1] + " waitingtime: " + result[2]);
            
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Invokes the getNumberOfCountries method on the server.
     * Updates the cache and writes the result to the output file.
     * 
     * @param args the arguments to the method
     */
    private void invokeGetNumberOfCountries(String []args) {
        try {
            int argsLen = args.length;
            int zone = (args[argsLen-1].charAt(5)) - '0';
            Registry registry = LocateRegistry.getRegistry();
            ProxyServerInterface proxyStub = (ProxyServerInterface) registry.lookup("Proxy");
            String serverName = proxyStub.getServer(zone);

            // connect to server
            ServerInterface stub = (ServerInterface) registry.lookup(serverName);
            
            if (args.length == 5) {
                long startTime = System.currentTimeMillis();
                long[] result = stub.getNumberofCountries(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                if (zone != Integer.parseInt(serverName.substring(6))) simulateLatency(90);
                long endTime = System.currentTimeMillis();
                long turnAroundTime = endTime - startTime;
       
                updateCache(String.join(" ", args), (int)result[0]);
                addResultToFile((int)result[0], args, turnAroundTime, result[1], result[2], serverName);
                System.out.println(String.join(" ", args) + ": " + result[0] + " --turnaroundtime: " + turnAroundTime + " executiontime: " + result[1] + " waitingtime: " + result[2]);
            } else {
                long startTime = System.currentTimeMillis();
                long[] result = stub.getNumberofCountries(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                if (zone != Integer.parseInt(serverName.substring(6))) simulateLatency(90);
                long endTime = System.currentTimeMillis();
                long turnAroundTime = endTime - startTime;
                updateCache(String.join(" ", args), (int)result[0]);
                addResultToFile((int)result[0], args, turnAroundTime, result[1], result[2], serverName);
                System.out.println(String.join(" ", args) + ": " + result[0] + " --turnaroundtime: " + turnAroundTime + " executiontime: " + result[1] + " waitingtime: " + result[2]);
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Adds the result of the query to the output file
     * @param result the result of the query
     * @param query the query
     * @param turnAroundTime the turn around time of the query
     * @param executionTime the execution time of the query
     * @param waitingTime the waiting time of the query
     * @param zone the zone of the server that executed the query
     */
    private synchronized void addResultToFile(int result, String[] query, long turnAroundTime, long executionTime, long waitingTime, String zone) {
        try {
            FileWriter fw = new FileWriter("naive_server.txt", true);
            fw.write(result + " " + String.join(" ", query) + " " + turnAroundTime + " " + executionTime + " " + waitingTime + " " + zone + "\n");
            fw.close();
        } catch (Exception e) {
            System.err.println("Error writing to output file in addResultToFile : " + e.getMessage());
        }
    }

    /**
     * Calculate the average time taken to execute a method
     */
    private void calculateAverageTime() {
        
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("naive_server.txt"));

            long[][] getPopulationOfCountryAvgTimes = new long[3][2];
            long[][] getNumberOfCitiesAvgTimes = new long[3][2];
            long[][] getNumberOfCountriesAvgTimes = new long[3][2];
            long[][] getNumberOfCountriesMaxAvgTimes = new long[3][2];

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(" ");
                int tokensLen = tokens.length;
                
                String methodName = tokens[1];
                if (methodName.equals("getNumberofCountries") && (tokensLen > 10)) methodName = methodName + "Max";
                
                long turnAroundTime = Long.parseLong(tokens[tokensLen - 4]);
                long executionTime = Long.parseLong(tokens[tokensLen - 3]);
                long waitingTime = Long.parseLong(tokens[tokensLen - 2]);
                

                switch(methodName) {
                    case "getPopulationofCountry":
                        updateTimes(getPopulationOfCountryAvgTimes, turnAroundTime, executionTime, waitingTime);
                        break;
                    case "getNumberofCities":
                        updateTimes(getNumberOfCitiesAvgTimes, turnAroundTime, executionTime, waitingTime);
                        break;
                    case "getNumberofCountries":
                        updateTimes(getNumberOfCountriesAvgTimes, turnAroundTime, executionTime, waitingTime);
                        break;
                    case "getNumberofCountriesMax":
                        updateTimes(getNumberOfCountriesMaxAvgTimes, turnAroundTime, executionTime, waitingTime);
                        break;
                    default:
                        System.out.println("Invalid method name: " + methodName);
                }
            }
            try {
                FileWriter fw = new FileWriter("naive_server.txt", true);
                writeAverage(fw, "getPopulationofCountry", getPopulationOfCountryAvgTimes);
                writeAverage(fw, "getNumberofCities", getNumberOfCitiesAvgTimes);
                writeAverage(fw, "getNumberofCountries", getNumberOfCountriesAvgTimes);
                writeAverage(fw, "getNumberofCountriesMax", getNumberOfCountriesMaxAvgTimes);
                fw.close();
            } catch (Exception e) {
                    System.err.println("Error writing to output file in calculateAverageTime : " + e.getMessage());
            }
         } catch (FileNotFoundException e) {
            System.err.println("File not found");
        }
        scanner.close();
    }

    /**
     * Updates the time table with the given times
     */
    void updateTimes(long[][] timeTable, long turnAroundTime, long executionTime, long waitingTime) {
        timeTable[0][0]+=turnAroundTime; timeTable[0][1]++;
        timeTable[1][0]+=executionTime; timeTable[1][1]++;
        timeTable[2][0]+=waitingTime; timeTable[2][1]++;
    }

    /**
     * Writes the average time to the output file
     * @param fw
     * @param methodName
     * @param timeTable
     * @throws IOException
     */
    void writeAverage(FileWriter fw, String methodName, long[][] timeTable) throws IOException {
        long turnAroundAvg = timeTable[0][0] / timeTable[0][1];
        long executionAvg = timeTable[1][0] / timeTable[1][1];
        long waitingAvg = timeTable[2][0] / timeTable[2][1];
        fw.write(methodName + " turnaround time:" + turnAroundAvg + "ms, execution time:" + executionAvg  + "ms, waiting time:" + waitingAvg + "ms\n");
    }

    /**
     * Writes the server caches to the output file server_cache.txt
     * Writes the client cache to the output file client_cache.txt
     * 
     * @throws Exception
     */
    void writeCacheToFile() {
        try {
            FileWriter fw = new FileWriter("server_cache.txt");
            Registry registry = LocateRegistry.getRegistry();
            ProxyServerInterface proxyStub = (ProxyServerInterface) registry.lookup("Proxy");

            List<LinkedHashMap<String, Integer>> serverCaches = proxyStub.getServerCaches();
            
            
            for (int i = 0; i < serverCaches.size(); i++) {
                LinkedHashMap<String, Integer> serverCache = serverCaches.get(i);
                fw.write("----- SERVER " + (i+1) + "-----\n");
                for (Map.Entry<String, Integer> entry : serverCache.entrySet()) {
                    fw.write(entry.getKey() + " " + entry.getValue() + "\n");
                }
            }
            fw.close();
        } catch (Exception e) {
            System.err.println("Error writing to output file in writeCacheToFile : " + e.getMessage());
        }

        try {
            FileWriter fw = new FileWriter("client_cache.txt");
            fw.write("----- CLIENT CACHE -----\n");
            for (Map.Entry<String, Integer> entry : cache.entrySet()) {
                fw.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
            fw.close();
        } catch (Exception e) {
            System.err.println("Error writing to output file in writeCacheToFile : " + e.getMessage());
        }
    }

    /**
     * Simulates latency
     * @param millis
     */
    private void simulateLatency(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates the outputfile
     * @param args
     */
    public static void main(String[] args) {
        Client client = new Client();
        File file = new File("naive_server.txt");
        if (file.exists()) file.delete();
        client.parseInputFile(args[0]);
        client.calculateAverageTime();
        client.writeCacheToFile();
    }
}