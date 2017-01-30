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

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private String bootstrapHost;

    private int bootstrapPort;

    private Node currentNode;

    private final List<Node> neighbours = new ArrayList<>();

    private final List<Query> queryList = new ArrayList<>();

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
            throw new IllegalArgumentException("Bootsrap IP is null");
        }
        if (Objects.isNull(nodeIP)) {
            throw new IllegalArgumentException("Node IP is null");
        }
        if (Objects.isNull(username) || "".equals(username.trim())) {
            throw new IllegalArgumentException("User name null");
        }

        if (!Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is already registered in the network.");
        }

        this.bootstrapHost = serverIP;
        this.bootstrapPort = serverPort;
        this.currentNode = new Node(nodeIP, port, username);

        String message = String.format(" REG %s %d %s", nodeIP, port, username);
        message = String.format("%04d", (message.length() + 4)) + message;

        try {
            String result = sendUdpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);

            StringTokenizer tokenizer = new StringTokenizer(result, " ");
            String length = tokenizer.nextToken();
            String command = tokenizer.nextToken();
            if (Command.REGOK.equals(command)) {
                int no_nodes = Integer.parseInt(tokenizer.nextToken());

                switch (no_nodes) {
                    case 0:

                        LOGGER.info("First node registered");
                        break;

                    case 1:
                        LOGGER.info("Second node registered");
                        String ipAddress = tokenizer.nextToken();
                        int portNumber = Integer.parseInt(tokenizer.nextToken());


                        Node nodeInfo = new Node(ipAddress, portNumber);
                        join(nodeInfo);
                        post(nodeInfo.url() + "join", new Node(nodeIP, port));
                        break;

                    case 2:
                        List<Node> returnedNodes = new ArrayList<>();
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
                        LOGGER.error("BootstrapServer limit exceeded. Register later");
                        this.currentNode = null;
                        return false;

                    case 9997:
                        LOGGER.error("IP and Port used by another node");
                        this.currentNode = null;
                        return false;

                    case 9998:
                        LOGGER.error("Already registered");
                        this.currentNode = null;
                        return false;

                    case 9999:
                        LOGGER.error("Error");
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

        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is not registered in BS");
        }

        final int peerSize = neighbours.size();
        for (int i = 0; i < peerSize; i++) {
            Node on = neighbours.get(i);
            if (on.equals(currentNode)) {
                continue;
            }
            for (int j = 0; j < peerSize; j++) {
                Node node = neighbours.get(j);
                if (i != j) {
                    post(on.url() + "join", node);
                }
            }
        }

        for (Node peer : neighbours) {
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

        if (!neighbours.contains(info)) {
            neighbours.add(info);
        }
    }


    synchronized List<Node> getPeers() {
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Register the node first");
        }
        return neighbours;
    }

    synchronized void leave(Node info) {

        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is not in the bootstrap");
        }

        neighbours.remove(info);
    }


    synchronized void initiateSearch(MovieList movieList, String name, int hopLimit) {
        if (Objects.isNull(movieList)) {
            throw new IllegalArgumentException("MovieList is null");
        }

        if (Objects.isNull(name) || "".equals(name.trim())) {
            throw new IllegalArgumentException("movie name null or empty");
        }

        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }


        Query query = new Query();
        query.setOrigin(currentNode);
        query.setQueryText(name);
        query.setHops(0);
        query.setSender(currentNode);
        query.setHopeLimit(hopLimit);
        query.setTimestamp(System.currentTimeMillis());

        queryList.add(query);

        List<String> results = movieList.search(query.getQueryText());

        Result result = new Result();
        result.setOwner(currentNode);
        result.setMovies(results);
        result.setHops(0);
        result.setTimestamp(query.getTimestamp());



        if(query.getHopeLimit()>query.getHops())
            for (Node peer : neighbours) {
                    post(peer.url() + "search", query);
            }

            post(query.getOrigin().url() + "results", result);

    }

    synchronized void search(MovieList movieList, Query query) {
        if (Objects.isNull(query) ) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("App is not registered in the bootstrap server");
        }

        if (queryList.contains(query)) {
            LOGGER.info("Duplicate query");
            return;
        } else {
            queryList.add(query);
        }

        // Increase the number of hops by one
        query.setHops(query.getHops() + 1);
        query.setSender(currentNode);

        Node sender = query.getSender();

        List<String> results = movieList.search(query.getQueryText());

        Result result = new Result();
        result.setOwner(currentNode);
        result.setMovies(results);
        result.setHops(query.getHops());
        result.setTimestamp(query.getTimestamp());


        if(query.getHopeLimit()>query.getHops())
            neighbours.stream().filter(peer -> !peer.equals(sender)).forEach(peer -> {
                post(peer.url() + "search", query);

            });
         post(query.getOrigin().url() + "results", result);
    }

    private void post(final String url, final Object object) {
        //LOGGER.info(url);
        new Thread() {
            @Override
            public void run() {
                try {
                    WebTarget target = ClientBuilder.newClient().target(url);
                    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                    Response response = builder.post(Entity.json(object));
                    //LOGGER.info(response.getStatus()+"");
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
