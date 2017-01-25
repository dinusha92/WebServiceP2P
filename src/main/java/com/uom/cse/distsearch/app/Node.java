package com.uom.cse.distsearch.app;

import com.uom.cse.distsearch.dto.NodeInfo;
import com.uom.cse.distsearch.dto.Query;
import com.uom.cse.distsearch.dto.Result;
import com.uom.cse.distsearch.util.Command;
import com.uom.cse.distsearch.util.IPAddressValidator;
import com.uom.cse.distsearch.util.MovieList;
import com.uom.cse.distsearch.util.Utility;
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
public class Node {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);

    //private final List<NodeInfo> peerList = new ArrayList<>();

    private final List<Query> queryList = new ArrayList<>();

    public String bootstrapHost;

    public int bootstrapPort;

    public NodeInfo currentNode,successor, predecessor;

    private static class InstanceHolder {
        private static Node instance = new Node();
    }

    private Node() {
    }

    public static Node getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Only use it for testing purposes.
     *
     * @return
     */
    static Node createInstance() {
        return new Node();
    }

    public synchronized void pJoin(NodeInfo info) {
//        // Validation
//        if (Objects.isNull(info)) {
//            throw new IllegalArgumentException("NodeInfo cannot be null");
//        }
//        if (Objects.equals(info.getIp(), currentNode.getIp()) && info.getPort() == currentNode.getPort()) {
//            throw new IllegalArgumentException("Cannot add this node as a peer of itself");
//        }
//
////        // State check
////        if (Objects.isNull(currentNode)) {
////            throw new InvalidStateException("Node is not registered in the bootstrap server");
////        }
//
////        LOGGER.debug("Adding {} as a peer of {}", info, currentNode);
////        if (!peerList.contains(info)) {
////            peerList.add(info);
////        }

        predecessor = info;
        post(predecessor.url() + "pjoin", currentNode);
    }

    public synchronized void sJoin(NodeInfo neighbour) {
//        // Validation
//        if (Objects.isNull(neighbour)) {
//            throw new IllegalArgumentException("NodeInfo cannot be null");
//        }
//        if (Objects.equals(neighbour.getIp(), currentNode.getIp()) && neighbour.getPort() == currentNode.getPort()) {
//            throw new IllegalArgumentException("Cannot add this node as a peer of itself");
//        }

//        // State check
//        if (Objects.isNull(currentNode)) {
//            throw new InvalidStateException("Node is not registered in the bootstrap server");
//        }

//        LOGGER.debug("Adding {} as a peer of {}", info, currentNode);
//        if (!peerList.contains(info)) {
//            peerList.add(info);
//        }


        post(predecessor.url() + "sjoin", neighbour);
//        String reply = " "+Command.SuccessorJOIN+" " + neighbour.getIp() + " " + neighbour.getPort();
//
//        String length_final = formatter.format(reply.length() + 4);
//        String final_reply = length_final  + reply;
//        send(new Communicator(receiver.getIp(),receiver.getPort(),final_reply));
    }
    public synchronized void leave(NodeInfo info) {
        // Validation
        if (Objects.isNull(info)) {
            throw new IllegalArgumentException("NodeInfo cannot be null");
        }

        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

        LOGGER.debug("Removing {} from the peer list of {}", info, currentNode);
        //peerList.remove(info);
    }


    public synchronized void startSearch(MovieList movieList, String name) {
        // Validation
        if (Objects.isNull(movieList)) {
            throw new IllegalArgumentException("MovieList cannot be null");
        }

        if (Objects.isNull(name) || "".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }


        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

        LOGGER.debug("Searching for {} on {}", name, currentNode);

        // Construct the repository
        Query qry = new Query();
        qry.setOrigin(currentNode);
        qry.setQuery(name);
        qry.setTimestamp(System.currentTimeMillis());
        qry.setHops(0);
        qry.setSender(currentNode);


        // Search within myself
        NodeInfo sender = qry.getSender();
        List<String> results = movieList.search(qry.getQuery());

        Result result = new Result();
        result.setOwner(currentNode);
        result.setMovies(results);
        result.setHops(0);
        result.setTimestamp(qry.getTimestamp());

        // Send the results
        post(qry.getOrigin().url() + "results", result);

        // Spread to the peers
//        for (NodeInfo peer : peerList) {
//            // Don't send to the sender again
//            if (!Objects.equals(peer, sender)) {
//                post(peer.url() + "search", qry);
//            }
//        }
    }

    public synchronized void search(MovieList movieList, Query query) {
        // Validation
        if (Objects.isNull(query) ) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        // State check
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

        if (queryList.contains(query)) {
            // Duplicate query
            return;
        } else {
            queryList.add(query);
        }

        // Increase the number of hops by one
        query.setHops(query.getHops() + 1);

        NodeInfo sender = query.getSender();

        List<String> results = movieList.search(query.getQuery());

        Result result = new Result();
        result.setOwner(currentNode);
        result.setMovies(results);
        result.setHops(query.getHops());
        result.setTimestamp(query.getTimestamp());

        // Send the results
        post(query.getOrigin().url() + "results", result);

        // Spread to the peers
//        for (NodeInfo peer : peerList) {
//            if (!peer.equals(sender)) {
//                LOGGER.debug("Sending request to {}", peer);
//                post(peer.url() + "search", query);
//            }
//        }
    }

    public synchronized boolean connect(String serverIP, int serverPort, String nodeIP, int port, String username) {
        // Validate
        if (Objects.isNull(serverIP)) {
            throw new IllegalArgumentException("Bootstrap server ip cannot be null");
        }
        if (Objects.isNull(nodeIP)) {
            throw new IllegalArgumentException("Node ip cannot be null");
        }
        if (Objects.isNull(username) || "".equals(username.trim())) {
            throw new IllegalArgumentException("username cannot be null or empty");
        }
        if (!IPAddressValidator.validate(serverIP)) {
            throw new IllegalArgumentException("Bootstrap server ip is not valid");
        }
        if (!IPAddressValidator.validate(nodeIP)) {
            throw new IllegalArgumentException("Node ip is not valid");
        }

//        // State check
//        if (!Objects.isNull(currentNode)) {
//            throw new InvalidStateException("Node is already registered.");
//        }

        this.bootstrapHost = serverIP;
        this.bootstrapPort = serverPort;
        this.currentNode = new NodeInfo(nodeIP, port, username);

        // Generate the command
        String message = String.format(" REG %s %d %s", nodeIP, port, username);
        message = String.format("%04d", (message.length() + 4)) + message;

        try {
            String result = Utility.sendTcpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);

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

                        NodeInfo nodeInfo = new NodeInfo(ipAddress, portNumber);
                        // JOIN to first node
                        pJoin(nodeInfo);
                        sJoin(currentNode);
                        break;

                    default:

                        LOGGER.debug("{} nodes registered", no_nodes);
                        List<NodeInfo> returnedNodes = new ArrayList<>();

                        // Select random 2 nodes
                        for (int i = 0; i < no_nodes; i++) {
                            String host = tokenizer.nextToken();
                            String hostPost = tokenizer.nextToken();
                            String userID = tokenizer.nextToken();

                            LOGGER.debug(String.format("%s:%s - %s", host, hostPost, userID));

                            NodeInfo node = new NodeInfo(host, Integer.parseInt(hostPost), userID);
                            returnedNodes.add(node);
                        }

                        Collections.shuffle(returnedNodes);

                        NodeInfo nodeA = returnedNodes.get(0);
                        NodeInfo nodeB = returnedNodes.get(1);

                        pJoin(nodeA);
                        post(nodeA.url() + "pJoin", this.currentNode);

                        pJoin(nodeB);
                        post(nodeB.url() + "pJoin", this.currentNode);
                        break;

                    case 9996:
                        LOGGER.error("Failed to register. BootstrapServer is full.");
                        this.currentNode = null;
                        return false;

                    case 9997:
                        LOGGER.error("Failed to register. This ip and port is already used by another Node.");
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
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

        // Update other nodes
//        final int peerSize = peerList.size();
//        for (int i = 0; i < peerSize; i++) {
//            NodeInfo on = peerList.get(i);
//            if (on.equals(currentNode)) {
//                continue;
//            }
//            for (int j = 0; j < peerSize; j++) {
//                NodeInfo node = peerList.get(j);
//                if (i != j) {
//                    post(on.url() + "pJoin", node);
//                }
//            }
//        }
//
//        for (NodeInfo peer : peerList) {
//            //send leave msg
//            post(peer.url() + "leave", currentNode);
//        }

        String message = String.format(" UNREG %s %d %s", currentNode.getIp(), currentNode.getPort(), currentNode.getUsername());
        message = String.format("%04d", (message.length() + 4)) + message;
        try {
            String result = Utility.sendTcpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);
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

//    public synchronized List<NodeInfo> getPeers() {
//        // State check
//        if (Objects.isNull(currentNode)) {
//            throw new InvalidStateException("Node is not registered in the bootstrap server");
//        }
//        return peerList;
//    }

    public void post(final String url, final Object object) {
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
