package com.uom.cse.distsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * @author kasun
 */
public class App {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private final List<Node> peerList = new ArrayList<>();

    //private final List<QueryInfo> queryList = new ArrayList<>();

    public String bootstrapHost;

    public int bootstrapPort;

    private Node currentNode;

    private static class InstanceHolder {
        private static App instance = new App();
    }

    private App() {
    }

    public static App getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Only use it for testing purposes.
     *
     * @return
     */
    static App createInstance() {
        return new App();
    }

    public synchronized void join(Node info) {
        // Validation
        if (Objects.isNull(info)) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        if (Objects.equals(info.getIp(), currentNode.getIp()) && info.getPort() == currentNode.getPort()) {
            throw new IllegalArgumentException("Cannot add this node as a peer of itself");
        }

        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }

        LOGGER.debug("Adding {} as a peer of {}", info, currentNode);
        if (!peerList.contains(info)) {
            peerList.add(info);
        }
    }

    public synchronized void leave(Node info) {
        // Validation
        if (Objects.isNull(info)) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }

        LOGGER.debug("Removing {} from the peer list of {}", info, currentNode);
        peerList.remove(info);
    }


    public synchronized void startSearch(MovieList movieList, String name, int hopeLimit) {
        // Validation
        if (Objects.isNull(movieList)) {
            throw new IllegalArgumentException("MovieList cannot be null");
        }

        if (Objects.isNull(name) || "".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }


        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }

        LOGGER.debug("Searching for {} on {}", name, currentNode);

        // Construct the repository
        //QueryInfo info = new QueryInfo();

        Query query = new Query();
        query.setOrigin(currentNode);
        query.setQuery(name);
        query.setTimestamp(System.currentTimeMillis());
        query.setHops(0);
        query.setSender(currentNode);
        query.setHopeLimit(hopeLimit);

        // Search within myself
        Node sender = query.getSender();
        List<String> results = movieList.search(query.getQuery());

        Result result = new Result();
        result.setOwner(currentNode);
        result.setMovies(results);
        result.setHops(0);
        result.setTimestamp(query.getTimestamp());


        int noOfSentNodes = 0;
        // Spread to the peers
        if(query.getHopeLimit()>query.getHops())
            for (Node peer : peerList) {
                    post(peer.url() + "search", query);
                    noOfSentNodes++;
            }

        // Send the results
        if(query.getHopeLimit()==query.getHops()||results.size()>0||noOfSentNodes==0) {
            post(query.getOrigin().url() + "results", result);
            LOGGER.info("search start");
        }else{
            LOGGER.info("hop limit {}",query.getHopeLimit());
            LOGGER.info("hops  {}",query.getHops());
            LOGGER.info("results  {}",results.size());
            LOGGER.info("no of nodes  {}",noOfSentNodes);
        }
    }

    public synchronized void search(MovieList movieList, Query query) {
        // Validation
        if (Objects.isNull(query) ) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }


        // Increase the number of hops by one
        query.setHops(query.getHops() + 1);
        query.setSender(currentNode);

        Node sender = query.getSender();

        List<String> results = movieList.search(query.getQuery());

        Result result = new Result();
        result.setOwner(currentNode);
        result.setMovies(results);
        result.setHops(query.getHops());
        result.setTimestamp(query.getTimestamp());



        int noOfSentNodes = 0;
        // Spread to the peers
        if(query.getHopeLimit()>query.getHops())
        for (Node peer : peerList) {
            if (!peer.equals(sender)&&!peer.equals(query.getOrigin())) {
                post(peer.url() + "search", query);
                noOfSentNodes++;
            }
        }
        // Send the results
        if(query.getHopeLimit()==query.getHops()||results.size()>0||noOfSentNodes==0) {
            post(query.getOrigin().url() + "results", result);
        }else{
            LOGGER.info("hop limit {}",query.getHopeLimit());
            LOGGER.info("hops  {}",query.getHops());
            LOGGER.info("results  {}",results.size());
            LOGGER.info("no of nodes  {}",noOfSentNodes);
        }
    }

    public synchronized boolean connect(String serverIP, int serverPort, String nodeIP, int port, String username) {
        LOGGER.info("Connect-app");
        // Validate
        if (Objects.isNull(serverIP)) {
            throw new IllegalArgumentException("Bootstrap server ip cannot be null");
        }
        if (Objects.isNull(nodeIP)) {
            throw new IllegalArgumentException("App ip cannot be null");
        }
        if (Objects.isNull(username) || "".equals(username.trim())) {
            throw new IllegalArgumentException("username cannot be null or empty");
        }

        // State check
        if (!Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is already registered.");
        }

        this.bootstrapHost = serverIP;
        this.bootstrapPort = serverPort;
        this.currentNode = new Node(nodeIP, port, username);

        // Generate the command
        String message = String.format(" REG %s %d %s", nodeIP, port, username);
        message = String.format("%04d", (message.length() + 4)) + message;

        try {
            String result = Utility.sendUdpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);

            LOGGER.debug("Connect response is {}", result);
            StringTokenizer tokenizer = new StringTokenizer(result, " ");
            String length = tokenizer.nextToken();
            String command = tokenizer.nextToken();
            if (Command.REGOK.equals(command)) {
                int no_nodes = Integer.parseInt(tokenizer.nextToken());

                switch (no_nodes) {
                    case 0:
                        // This is the first node registered to the BootstrapServer.
                        // Do nothing
                        LOGGER.debug("First node registered");
                        break;

                    case 1:
                        LOGGER.debug("Second node registered");
                        String ipAddress = tokenizer.nextToken();
                        int portNumber = Integer.parseInt(tokenizer.nextToken());
                        // TODO: Test the following line
                        //String userName = tokenizer.nextToken();

                        Node nodeInfo = new Node(ipAddress, portNumber);
                        // JOIN to first node
                        join(nodeInfo);
                        post(nodeInfo.url() + "join", new Node(nodeIP, port));
                        break;

                    default:

                        LOGGER.debug("{} nodes registered", no_nodes);
                        List<Node> returnedNodes = new ArrayList<>();

                        // Select random 2 nodes
                        for (int i = 0; i < no_nodes; i++) {
                            String host = tokenizer.nextToken();
                            String hostPost = tokenizer.nextToken();

                            LOGGER.debug(String.format("%s:%s ", host, hostPost));

                            Node node = new Node(host, Integer.parseInt(hostPost));
                            returnedNodes.add(node);
                        }

                        Collections.shuffle(returnedNodes);

                        Node nodeA = returnedNodes.get(0);
                        Node nodeB = returnedNodes.get(1);

                        join(nodeA);
                        post(nodeA.url() + "join", currentNode);

                        join(nodeB);
                        post(nodeB.url() + "join", currentNode);
                        break;

                    case 9996:
                        LOGGER.error("Failed to register. BootstrapServer is full.");
                        this.currentNode = null;
                        return false;

                    case 9997:
                        LOGGER.error("Failed to register. This ip and port is already used by another App.");
                        this.currentNode = null;
                        return false;

                    case 9998:
                        LOGGER.error("You are already registered. Please unregister first.");
                        this.currentNode = null;
                        return false;

                    case 9999:
                        LOGGER.error("Error in the command. Please fix the error");
                        this.currentNode = null;
                        return false;
                }

                return true;
            } else {
                this.currentNode = null;
                return false;
            }

        } catch (IOException e) {
            this.currentNode = null;
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public synchronized boolean disconnect() {
        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }

        // Update other nodes
        final int peerSize = peerList.size();
        for (int i = 0; i < peerSize; i++) {
            Node on = peerList.get(i);
            if (on.equals(currentNode)) {
                continue;
            }
            for (int j = 0; j < peerSize; j++) {
                Node node = peerList.get(j);
                if (i != j) {
                    post(on.url() + "join", node);
                }
            }
        }

        for (Node peer : peerList) {
            //send leave msg
            post(peer.url() + "leave", currentNode);
        }

        String message = String.format(" UNREG %s %d %s", currentNode.getIp(), currentNode.getPort(), currentNode.getUsername());
        message = String.format("%04d", (message.length() + 4)) + message;
        try {
            String result = Utility.sendUdpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);
            StringTokenizer tokenizer = new StringTokenizer(result, " ");
            String length = tokenizer.nextToken();
            String command = tokenizer.nextToken();
            if (Command.UNROK.equals(command)) {
                this.currentNode = null;
                return true;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    public synchronized List<Node> getPeers() {
        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }
        return peerList;
    }

    private void post(final String url, final Object object) {
        LOGGER.debug("POST URL: {}", url);
        new Thread() {
            @Override
            public void run() {
                try {
                    WebTarget target = ClientBuilder.newClient().target(url);
                    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                    Response response = builder.post(Entity.json(object));
                    int status = response.getStatus();
                    LOGGER.debug("Status: {}", status);
                    Object str = response.getEntity();
                    LOGGER.debug("Message: {}", str);
                    response.close();
                } catch (Exception ex) {
                    LOGGER.error("Exception in sending request", ex.getMessage());
                }
            }
        }.start();
    }
}
