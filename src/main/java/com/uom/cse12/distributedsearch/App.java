package com.uom.cse12.distributedsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
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
    int receivedMessages, sentMessages, unAnsweredMessages;
    private List<Integer> latencyArray = new ArrayList<>();
    private List<Integer> hopArray = new ArrayList<>();
    private int localResultCounter=0;
    private String localQuery ="";
    private List<String> LocalQueries=new ArrayList<>();
    private int queryPointer =0;
    private  int noOfNodesInTheNetwork =0;
    MovieList movieList;

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
            throw new IllegalArgumentException("Bootstrap IP is null");
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


                        Node node = new Node(ipAddress, portNumber);
                        join(node);
                        post(node.url() + "join", new Node(nodeIP, port));
                        break;

                    case 2:
                        List<Node> returnedNodes = new ArrayList<>();
                        for (int i = 0; i < no_nodes; i++) {
                            String host = tokenizer.nextToken();
                            String hostPost = tokenizer.nextToken();

                            LOGGER.debug(String.format("%s:%s ", host, hostPost));

                            Node temp = new Node(host, Integer.parseInt(hostPost));
                            returnedNodes.add(temp);
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

    void onResultReceived(Result result){
        int moviesCount = result.getMovies().size();

        long latency = (System.currentTimeMillis() - result.getTimestamp());


        latencyArray.add((int) latency);
        hopArray.add(result.getHops());
        String output = "\n\nq= " +queryPointer+" **Result : "+ ++localResultCounter +"  [ Query = "+ localQuery +"]" ;
        output += String.format("Number of movies: %d\nMovies: %s\nHops: %d\nSender %s:%d\nLatency: %s ms",
                moviesCount, result.getMovies().toString(), result.getHops(), result.getOwner().getIp(), result.getOwner().getPort(), latency);
        LOGGER.info(output);

        if(localResultCounter==noOfNodesInTheNetwork&&LocalQueries.size()>queryPointer){
            startQurey(LocalQueries.get(queryPointer++),movieList);
        }else if(LocalQueries.size()<=queryPointer){
            System.out.println("****Searching completed!****");
        }
    }
    void startQurey(String qry,MovieList ml){
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    initiateSearch(ml,qry,999999);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    void remoteQery(String file,MovieList ml,int nodes){
        movieList = ml;
        LocalQueries=new ArrayList<>();
        noOfNodesInTheNetwork = nodes;
        queryPointer =0;

        String fileName = file;
        try {
            Scanner scanner = new Scanner(new File(fileName));
            while (scanner.hasNextLine()) {
                LocalQueries.add(scanner.nextLine().trim().toLowerCase().replace(" ","_"));
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        //initiateSearch(tokenizer.nextToken());
        if(LocalQueries.size()>0)
            startQurey(LocalQueries.get(queryPointer++),ml);
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
    synchronized void join(Node node) {

        if (!neighbours.contains(node)) {
            neighbours.add(node);
        }
    }


    synchronized List<Node> getPeers() {
        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Register the node first");
        }
        return neighbours;
    }

    synchronized void leave(Node node) {

        if (Objects.isNull(currentNode)) {
            throw new InvalidStateException("Node is not in the bootstrap");
        }

        neighbours.remove(node);
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

        localResultCounter=0;
        localQuery = name;

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
            unAnsweredMessages++;
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

        LOGGER.info("\n\n*sent stats to "+query.getOrigin());
         post(query.getOrigin().url() + "results", result);
    }

    Stat getStats(){
        Stat stat = new Stat();
        stat.setAnsweredMessages(receivedMessages- unAnsweredMessages);
        stat.setSentMessages(sentMessages);
        stat.setReceivedMessages(receivedMessages);
        stat.setNodeDegree(neighbours.size());
        if(latencyArray.size()>0){
            double avg = latencyArray.stream().mapToLong(val -> val).average().getAsDouble();
            stat.setLatencyMax(Collections.max(latencyArray));
            stat.setLatencyMin(Collections.min(latencyArray));
            stat.setLatencyAverage(avg);
            stat.setLatencySD(getSD(latencyArray.toArray(), avg));
            stat.setNumberOfLatencies(latencyArray.size());

            String latencies="";
            for (int latency: latencyArray){
                latencies+=latency+",";
            }
            stat.setLatencies(latencies);


            avg = hopArray.stream().mapToLong(val -> val).average().getAsDouble();
            stat.setHopsMax(Collections.max(hopArray));
            stat.setHopsMin(Collections.min(hopArray));
            stat.setHopsAverage(avg);
            stat.setHopsSD(getSD(hopArray.toArray(), avg));
            stat.setNumberOfHope(hopArray.size());
            String hops="";
            for (int hop: hopArray){
                hops+=hop+",";
            }
            stat.setHops(hops);

        }
        return stat;
    }



    void clearStats(){
        receivedMessages=0;
        sentMessages= 0;
        unAnsweredMessages = 0;
        latencyArray= new ArrayList<>();
        hopArray = new ArrayList<>();
    }

    private double getSD(Object[] latency, double mean){
        double variance = 0, sd =0;
        double [] temp =  new double[latency.length];
        for (int i = 0; i < latency.length; i++) {
            temp[i] = (double)(Integer)latency[i] - mean;
            temp[i] = Math.pow(temp[i], 2); //to get the (x-average)2
            variance += temp[i];
        }
        variance = variance / (latency.length-1); // sample variance
        sd = Math.sqrt(variance);
        return sd;
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
                    sentMessages++;
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
        LOGGER.info("Received :" + msg);
        return msg;
    }

    private class InvalidStateException extends RuntimeException {
        InvalidStateException(String msg) {
            super(msg);
        }
    }
}
