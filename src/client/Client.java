package client;

import common.Logger;
import common.Request;
import common.Response;
import common.ServerInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Client {
    public final static common.Logger logger = new Logger();
    private final List<Request> requests;

    /**
     * Constructor.
     */
    public Client() {
        this.requests = new ArrayList<>();
        this.prePopulate();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            logger.log(Level.SEVERE, "Using: <Host Name> <Port Number>");
            System.exit(1);
        }

        while (true) {
            // Initialization.
            Client client = new Client();
            String host = args[0];
            int serverPort = Integer.parseInt(args[1]);
            try {
                ServerInterface server = (ServerInterface) Naming.lookup("rmi://host.docker.internal:" + serverPort + "/Server");
                // ServerInterface server = (ServerInterface) Naming.lookup("rmi://localhost:" + serverPort + "/Server");
                logger.log(Level.INFO, "Connecting to the server at Port: " + serverPort);

                // Prepopulate the data.
                for (Request request : client.getRequests()) {
                    Response response = server.put(request);
                    logger.log(Level.INFO, "The pre-populate result given: " + response);
                }

                // Use while loop to make the client continue to send requests.
                while (true) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Please enter the request in a format as method: PUT|GET|DELETE, key:, [value:]");
                    String requestInput = null;
                    try {
                        requestInput = reader.readLine();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Readline error: " + e);
                    }

                    try {
                        Request currRequest = Request.createRequest(requestInput);
                        if (currRequest.getMethod() == Request.Method.PUT) {
                            Response response = server.put(currRequest);
                            logger.log(Level.INFO, "Response: " + response);
                        } else if (currRequest.getMethod() == Request.Method.GET) {
                            Response response = server.get(currRequest);
                            logger.log(Level.INFO, "Response: " + response);
                        } else if (currRequest.getMethod() == Request.Method.DELETE) {
                            Response response = server.delete(currRequest);
                            logger.log(Level.INFO, "Response: " + response);
                        } else {
                            logger.log(Level.SEVERE, "Please enter a valid request.");
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error on creating a request: " + e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "Remote connection failed, trying again in 5 seconds.");
                // Wait for 5 seconds before trying to establish the connection.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Getter.
     *
     * @return
     */
    public List<Request> getRequests() {
        return requests;
    }

    /**
     * Pre-populate 5 pairs of key-value store.
     */
    public void prePopulate() {
        Request request1 = new Request(Request.Method.PUT, "Jesse", "75");
        Request request2 = new Request(Request.Method.PUT, "Gus", "60");
        Request request3 = new Request(Request.Method.PUT, "Chris", "80");
        Request request4 = new Request(Request.Method.PUT, "Julian", "95");
        Request request5 = new Request(Request.Method.PUT, "Ben", "101");
        this.requests.add(request1);
        this.requests.add(request2);
        this.requests.add(request3);
        this.requests.add(request4);
        this.requests.add(request5);
    }
}
