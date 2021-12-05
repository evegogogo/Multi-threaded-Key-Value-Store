package server;

import common.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Server that connects with the client.
 * Implements PAXOS.
 */
public class Server extends UnicastRemoteObject implements ServerInterface {
    public final static common.Logger logger = new common.Logger();
    ConcurrentHashMap<String, String> keyValueStore;
    private String serverID;
    private final Registry registry;
    private final int port;

    private long prevProposalNum;
    private long lastLearnedProposalNum;
    private Request prevAcceptedValue;

    // Used to configure the acceptors to fail at random times.
    private final long randomAcceptorFailureNum = 10l;
    // Used to limit the Paxos retry.
    private final int maxPaxosRetry = 3;

    /**
     * Constructor.
     *
     * @param serverID
     * @param registry
     * @param port
     * @throws RemoteException
     */
    public Server(String serverID, Registry registry, int port) throws RemoteException {
        super();
        this.keyValueStore = new ConcurrentHashMap<>();
        this.serverID = serverID;
        this.registry = registry;
        this.port = port;
    }

    /**
     * Create a new Server at the localhost.
     *
     * @param port
     * @return
     */
    public static String createServerID(int port) {
        String id = null;
        try {
            InetAddress host = InetAddress.getLocalHost();
            id = host.getHostAddress() + "_" + port;
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Unknown Host Exception: " + e);
        }
        return id;
    }

    public static void main(String[] args) {
        // Used for Docker.
        String[] discoveryNodes = new String[]{"host.docker.internal:4444", "host.docker.internal:5555", "host.docker.internal:6666", "host.docker.internal:7777", "host.docker.internal:8888"};

        try {
            int port = Integer.parseInt(args[0]);
            // Rmiregistry within the server JVM with port number 1099.
            Registry registry = LocateRegistry.createRegistry(port);
            String currServerID = createServerID(port);
            Server currServer = new Server(currServerID, registry, port);
            // Bind the remote object by the name "Server".
            registry.rebind("Server", currServer);
            logger.log(Level.INFO, "Server started at port: " + port);

            // Prepare the nodes needed on the config.properties file.
            // InputStream input = new FileInputStream("resources/config.properties");
//            Properties prop = new Properties();
//            prop.load(input);
//            // Get the 5 discovery nodes to connect to the cluster.
//            String[] discoveryNodes = prop.getProperty("discovery.nodes").split(",");

            boolean discoverySucceed = false;
            logger.log(Level.INFO, "Server is trying to connect to a cluster.");

            for (String discoveryNode : discoveryNodes) {
                try {
                    String[] data = discoveryNode.split(":");
                    String discoveryNodeHost = data[0];
                    int discoveryNodePort = Integer.parseInt(data[1]);

                    // Get a remote object for each node.
                    Registry discoveryRegistry = LocateRegistry.getRegistry(discoveryNodeHost, discoveryNodePort);
                    //if (!currServer.tryToBind(discoveryRegistry, ))
                    for (String serverID : discoveryRegistry.list()) {
                        try {
                            ServerInterface discoveryRegistryServer = (ServerInterface) discoveryRegistry.lookup(serverID);
                            // Register the node with the proxy server.
                            if (!currServerID.equals(discoveryRegistryServer.getServerID())) {
                                discoverySucceed = true;
                                currServer.setStorage(discoveryRegistryServer.getStorage());
                                discoveryRegistryServer.registerNewServer(currServerID, currServer);
                                logger.log(Level.INFO, "Registered current server with server: " + discoveryRegistryServer.getServerID());
                                registry.bind(discoveryRegistryServer.getServerID(), discoveryRegistryServer);
                                logger.log(Level.INFO, "Registered the server with the current server: " + discoveryRegistryServer.getServerID());
                            }
                        } catch (ConnectException e) {
                            continue;
                        }
                    }
                    if (discoverySucceed == true) {
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            if (!discoverySucceed) {
                logger.log(Level.INFO, "Could not connect to any clusters, acting as a standalone cluster.");
            } else {
                logger.log(Level.INFO, "Connected to a cluster.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Server error: " + e);
            System.exit(0);
        }
    }

    /**
     * Bind the remote object by the serverID.
     *
     * @param currServerID
     * @param server
     * @throws RemoteException
     */
    public void registerNewServer(String currServerID, ServerInterface server) throws RemoteException {
        registry.rebind(currServerID, server);
        logger.log(Level.INFO, "Registered a new server: " + currServerID);
    }

    /**
     * Getter.
     *
     * @return
     * @throws RemoteException
     */
    public String getServerID() throws RemoteException {
        return serverID;
    }

    /**
     * Setter.
     *
     * @param serverID
     */
    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    /**
     * Getter.
     *
     * @return
     * @throws RemoteException
     */
    public ConcurrentHashMap<String, String> getStorage() throws RemoteException {
        return keyValueStore;
    }

    /**
     * Setter.
     *
     * @param storage
     */
    public void setStorage(ConcurrentHashMap<String, String> storage) {
        this.keyValueStore = storage;
    }

    /**
     * GET operation.
     *
     * @param request
     * @return
     * @throws RemoteException
     */
    public synchronized Response get(Request request) throws RemoteException {
        logger.log(Level.INFO, "Received a new request: " + request.toString());
        String key = request.getKey();
        Response response = null;
        if (!keyValueStore.containsKey(key)) {
            response = new Response("400", Response.Status.FAILED, "");
            logger.log(Level.INFO, "The key does not exist: " + key);
        } else {
            String value = keyValueStore.get(key);
            response = new Response("200", Response.Status.SUCCEED, value);
            logger.log(Level.INFO, "The value has been found: " + value);
        }
        return response;
    }

    /**
     * PUT operation.
     *
     * @param request
     * @throws RemoteException
     */
    public synchronized Response put(Request request) throws RemoteException {
        logger.log(Level.INFO, "Received a new request: " + request.toString());
        String key = request.getKey();
        String value = request.getValue();
        Response response = null;

        // At first, invoke the proposer.
        logger.log(Level.INFO, "Invoking the Proposer.");
        try {
            invokeProposer(request);
            response = new Response("200", Response.Status.SUCCEED, value);
            logger.log(Level.INFO, "The pair of key and value has been stored.");
        } catch (TimeoutException e) {
            response = new Response("500", Response.Status.FAILED, value);
            logger.log(Level.SEVERE, "Time out: " + e);
        }
        return response;
    }

    /**
     * DELETE operation.
     *
     * @param request
     * @return
     * @throws RemoteException
     */
    public synchronized Response delete(Request request) throws RemoteException {
        logger.log(Level.INFO, "Received a new request: " + request.toString());
        String key = request.getKey();
        Response response = null;

        // At first, invoke the proposer.
        logger.log(Level.INFO, "Invoking the Proposer.");
        if (!keyValueStore.containsKey(key)) {
            response = new Response("400", Response.Status.FAILED, "");
            logger.log(Level.INFO, "The key does not exist: " + key);
        } else {
            try {
                invokeProposer(request);
                response = new Response("200", Response.Status.SUCCEED, "");
                logger.log(Level.INFO, "The pair of key and value has been deleted.");
            } catch (TimeoutException e) {
                response = new Response("500", Response.Status.FAILED, "");
                logger.log(Level.SEVERE, "Time out: " + e);
            }
        }
        return response;
    }

    /**
     * PAXOS Processes.
     *
     * @param request
     * @throws TimeoutException
     * @throws RemoteException
     */
    public void invokeProposer(Request request) throws TimeoutException, RemoteException {
        boolean roundFailed = true;
        int tried = 1;

        // Will try Paxos for 3 rounds until succeed.
        while (roundFailed) {
            if (tried > maxPaxosRetry) {
                throw new TimeoutException("The maximum retries of Paxos has been reached.");
            }
            tried++;

            logger.log(Level.INFO, "A new Paxos round starts.");

            // Set a unique proposal number based on the time.
            long proposalNum = System.currentTimeMillis();
            logger.log(Level.INFO, "The proposer sets a unique proposal number: " + proposalNum);

            // Keep a list of promises to store the result.
            List<Promise> promises = new ArrayList<>();
            // Phase 1: Prepare-Promise.
            for (String serverID : registry.list()) {
                logger.log(Level.INFO, "Sending a prepare message to the server: " + serverID);
                try {
                    ServerInterface currServer = (ServerInterface) registry.lookup(serverID);
                    // Sending a prepare message to the acceptor.
                    // Receiving a promise message from the acceptor.
                    Promise currPromise = currServer.prepare(proposalNum);
                    logger.log(Level.INFO, "Received a promise.");
                    currPromise.setServerID(serverID);
                    promises.add(currPromise);
                } catch (NotBoundException e) {
                    logger.log(Level.SEVERE, "Not Bound Exception: " + e);
                }
            }

            // Check whether the proposer received a majority of promises.
            // If the promises amount doesn't reach the majority, do not response.
            if (promises.size() <= registry.list().length / 2) {
                logger.log(Level.INFO, "Didn't receive a majority of promises. Restarting a new Paxos round.");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Interrupted Exception: " + e);
                }
                continue;
            }

            // If yes.
            logger.log(Level.INFO, "Received a majority of promises.");

            // Phase 2: Propose-accept.
            long maxProposalNum = 0l;
            Request value = request;
            for (Promise promise : promises) {
                long prevProposalNum = promise.getPrevProposalNum();
                if (prevProposalNum != 0 && prevProposalNum > maxProposalNum) {
                    maxProposalNum = prevProposalNum;
                    value = promise.getPrevAcceptedValue();
                }
            }

            logger.log(Level.INFO, "Value for accepted: " + value.toString());
            List<Accept> accepteds = new ArrayList<>();

            for (Promise promise : promises) {
                String currServerID = promise.getServerID();
                logger.log(Level.INFO, "Sending an accept message to the server: " + currServerID);
                try {
                    ServerInterface currServer = (ServerInterface) registry.lookup(currServerID);
                    Accept currAccepted = currServer.accept(proposalNum, value);
                    logger.log(Level.INFO, "Received an accept.");
                    currAccepted.setServerID(currServerID);
                    accepteds.add(currAccepted);
                } catch (NotBoundException e) {
                    logger.log(Level.SEVERE, "Not Bound Exception: " + e);
                }
            }

            // Check whether the proposer received a majority of accepted.
            // If the accepted messages doesn't reach the majority, do not response.
            if (accepteds.size() <= registry.list().length / 2) {
                logger.log(Level.INFO, "Didn't receive a majority of accepted. Restarting a new Paxos round.");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Interrupted Exception: " + e);
                    e.printStackTrace();
                }
                continue;
            }

            // If yes.
            logger.log(Level.INFO, "Received a majority of accpeted.");

            // Sending the accpeted message to learners.
            logger.log(Level.INFO, "Invoking Learners.");
            for (Accept accepted : accepteds) {
                String currServerID = accepted.getServerID();
                logger.log(Level.INFO, "Invoking learner: " + currServerID);
                try {
                    ServerInterface currServer = (ServerInterface) registry.lookup(currServerID);
                    currServer.invokeLearner(accepted);
                    logger.log(Level.INFO, "Learner was successfully invoked.");
                } catch (NotBoundException e) {
                    logger.log(Level.SEVERE, "Not Bound Exception" + e);
                }
            }
            logger.log(Level.INFO, "Learning completed.");
            roundFailed = false;
        }
        logger.log(Level.INFO, "The Paxos round ended.");
    }

    /**
     * Prepare the promise.
     *
     * @param proposalNum
     * @return
     * @throws RemoteException
     */
    public Promise prepare(long proposalNum) throws RemoteException {
        // The acceptor is configured to fail at random times.
        if (proposalNum % randomAcceptorFailureNum == 0l) {
            logger.log(Level.INFO, "The acceptor is configured to fail at random times.");
            throw new RemoteException("The acceptor is failed.");
        }

        // If the prepare request doesn't reach the rule, don't respond.
        if (proposalNum <= prevProposalNum) {
            logger.log(Level.INFO, "The prepare request is rejected, because the proposal number is less than the previous proposal number.");
            throw new RemoteException("The prepare request is rejected.");
        }

        // If the prepare request passes the rule, then create a promise to send back.
        // Store the proposal number and the value.
        Promise promise = new Promise();
        promise.setProposalNum(proposalNum);
        promise.setPrevProposalNum(prevProposalNum);
        promise.setPrevAcceptedValue(prevAcceptedValue);

        logger.log(Level.INFO, "Sending a promise for the proposal: " + proposalNum);
        return promise;
    }

    /**
     * Accept the proposal.
     *
     * @param proposalNum
     * @param request
     * @return
     * @throws RemoteException
     */
    public Accept accept(long proposalNum, Request request) throws RemoteException {
        // The acceptor is configured to fail at random times.
        if (proposalNum % randomAcceptorFailureNum == 0l) {
            logger.log(Level.INFO, "The acceptor is configured to fail at random times.");
            throw new RemoteException("The acceptor is failed.");
        }

        // If the prepare request doesn't reach the rule, don't respond.
        if (proposalNum < prevProposalNum) {
            logger.log(Level.INFO, "The accept request is rejected, because the proposal number is less than the previous proposal number.");
            throw new RemoteException("The accept request is rejected.");
        }

        logger.log(Level.INFO, "The accept request is confirmed: " + request.toString());

        // If the accept request passes the rule, then create a accepted message to send back.
        Accept accepted = new Accept();
        accepted.setProposalNum(proposalNum);
        accepted.setValue(request);
        return accepted;
    }

    /**
     * Invoke the learner.
     * Call the PUT and DELETE.
     *
     * @param accepted
     * @throws RemoteException
     */
    public synchronized void invokeLearner(Accept accepted) throws RemoteException {
        logger.log(Level.INFO, "Learner was invoked.");

        if (lastLearnedProposalNum == accepted.getProposalNum()) {
            logger.log(Level.INFO, "The value has been learned.");
            throw new RemoteException("The value has been learned.");
        }

        if (accepted.getServerID() == serverID) {
            logger.log(Level.INFO, "Reset the previous proposal number and accepted value.");
            prevProposalNum = 0;
            prevAcceptedValue = null;
        }

        Request currRequest = accepted.getValue();
        if (currRequest.getMethod().equals(Request.Method.PUT)) {
            keyValueStore.put(currRequest.getKey(), currRequest.getValue());
        } else if (currRequest.getMethod().equals(Request.Method.DELETE)) {
            keyValueStore.remove(currRequest.getKey());
        }

        lastLearnedProposalNum = accepted.getProposalNum();
        logger.log(Level.INFO, "Learned a new value: " + currRequest);
    }

    /**
     * Used for LocateRegistry.getRegistry.
     *
     * @param registry
     * @param name
     * @param object
     * @return
     */
    public boolean tryToBind(Registry registry, String name, Remote object) {
        try {
            registry.bind(name, object);
        } catch (AlreadyBoundException | RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

