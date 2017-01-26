package com.uom.cse12.distributedsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;


class App {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private final List<Node> peerList = new ArrayList<>();


    private String bootstrapHost;

    private int bootstrapPort;

    private Node currentNode;

    private static class InstanceHolder {
        private static App instance = new App();
    }

    private App() {
    }

    static App getInstance() {
        return InstanceHolder.instance;
    }


    synchronized boolean connect(String serverIP, int serverPort, String nodeIP, int port, String username) {
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
            String result = sendUdpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);

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


    synchronized boolean disconnect() {
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
            String result = sendUdpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);
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
    synchronized void join(Node info) {
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


    synchronized List<Node> getPeers() {
        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }
        return peerList;
    }

    synchronized void leave(Node info) {
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


    synchronized void initiateSearch(MovieList movieList, String name, int hopeLimit) {
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

        Query query = new Query();
        query.setOrigin(currentNode);
        query.setQuery(name);
        query.setHops(0);
        query.setSender(currentNode);
        query.setHopeLimit(hopeLimit);

        // Search within myself
        List<String> results = movieList.search(query.getQuery());

        Result result = new Result();
        result.setOwner(currentNode);
        result.setMovies(results);
        result.setHops(0);


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
        }
    }

    synchronized void search(MovieList movieList, Query query) {
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
        }
    }

    private void post(final String url, final Object object) {
        new Thread() {
            @Override
            public void run() {
                try {
                    WebTarget target = ClientBuilder.newClient().target(url);
                    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                    Response response = builder.post(Entity.json(object));
                    response.close();
                } catch (Exception ex) {
                    LOGGER.error("Exception in sending request", ex.getMessage());
                }
            }
        }.start();
    }

    private  String sendUdpToBootstrapServer(String message, String ip, int port) throws IOException {

        DatagramSocket sock = new DatagramSocket(45654);

        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(ip), port);
        sock.send(packet);
        byte[] buffer = new byte[65536];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        sock.receive(response);
        sock.close();

        byte[] data = response.getData();
        String msg = new String(data, 0, response.getLength());
        LOGGER.info("Recieved :" + msg);
        return msg;
    }

    private class InvalidStateException extends RuntimeException {
        InvalidStateException(String msg) {
            super(msg);
        }
    }
}
