package com.uom.cse12.distributedsearch;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

/**
 * Created by kasun on 2/6/17.
 */
public class RemoteWebServiceQueryHandler {

    public static void main(String[] args) {
//        DatagramSocket socket = null;
        List<String> queryList = new ArrayList<>();
        List<Node> nodeList = new ArrayList<>();

        nodeList.add(new Node("128.199.142.217",50001));
        nodeList.add(new Node("128.199.151.76",50002));
        nodeList.add(new Node("128.199.160.17",50003));
        nodeList.add(new Node("128.199.161.29",50004));
        nodeList.add(new Node("188.166.226.16",50005));
        nodeList.add(new Node("188.166.223.190",50006));
        nodeList.add(new Node("188.166.217.130",50007));
        nodeList.add(new Node("188.166.211.138",50008));
        nodeList.add(new Node("188.166.215.16",50009));
        nodeList.add(new Node("188.166.215.150",50010));

//        nodeList.add(new Node("127.0.0.1", 50000));
//        nodeList.add(new Node("127.0.0.1", 50001));
//        nodeList.add(new Node("127.0.0.1", 50002));
//        nodeList.add(new Node("127.0.0.1", 50003));
//        nodeList.add(new Node("127.0.0.1", 50004));
//        nodeList.add(new Node("127.0.0.1", 50005));
//        nodeList.add(new Node("127.0.0.1", 50006));
//        nodeList.add(new Node("127.0.0.1", 50007));
//        nodeList.add(new Node("127.0.0.1", 50008));
//        nodeList.add(new Node("127.0.0.1", 50009));


        int qhport = 54000;
        String fileName = "Queries.txt";
        boolean stay = true;
        List<Stat> statList = new ArrayList<>();
        int counttt = 0;
        try {
            Scanner scanner = new Scanner(new File(fileName));
            while (scanner.hasNextLine()) {
                queryList.add(scanner.nextLine());
                counttt++;
//                if(counttt>2){
//                    break;
//                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(queryList);
        try {
//            socket = new DatagramSocket(qhport);
            System.out.println("Query Handler Activated on port " + qhport);
            int option = 0;
            String reply = null;
            int query_position = 0;
            int node_position = 0;
            int fileNumber = 0;

            int max_rec = 0;
            int max_ans = 0;
            int max_sent = 0;
            int max_ndgree = 0;

            int min_rec = 0;
            int min_ans = 0;
            int min_sent = 0;
            int min_ndgree = 0;

            double avg_rec = 0;
            double avg_sent = 0;
            double avg_ans = 0;
            double avg_ndgree = 0;

            double sd_rec = 0;
            double sd_ans = 0;
            double sd_sent = 0;
            double sd_ndgree = 0;

            int hop_min = 9999999;
            int hop_max = 0;
            double hop_sd = 0;
            double hop_average = 0;
            double latency_min = 9999999;
            double latency_max = 0;
            double latency_sd = 0;
            double latency_average = 0;
            String hops = "", latencies = "";


            List<Integer> received_stat = new ArrayList<>();
            List<Integer> answered_stat = new ArrayList<>();
            List<Integer> sent_stat = new ArrayList<>();
            List<Integer> node_degree = new ArrayList<>();


            while (stay) {
                System.out.println("-----------------Instruction--------------");
                System.out.println("1.Clear stat");
                System.out.println("9.Query next movie");
                System.out.println("3.Select node to send query: Default value 0");
                System.out.println("4.Get stat after 50 queries");
                System.out.println("5.Final Stat Results after 250 queries");
                System.out.println("6.Remove node from");
                System.out.println("7.Exit");

                Scanner scan = new Scanner(System.in);
                int len = nodeList.size();
                try {
                    option = Integer.parseInt(scan.nextLine().trim());
                } catch (NumberFormatException e) {
                    option = -1;
                }
                switch (option) {
                    case 1:
                        reply = "0010 CLEAR";
                        String answer = null;
                        System.out.println("y/n??");
                        answer = (scan.nextLine().trim());
                        if (!answer.equals("y")) {
                            System.out.println("Not cleared");
                            break;
                        }
                        query_position = 0;
                        for (int i = 0; i < len; i++) {
//                            DatagramPacket dpReply = new DatagramPacket(reply.getBytes(), reply.getBytes().length, InetAddress.getByName(nodeList.get(i).getIp()), nodeList.get(i).getPort());
//                            socket.send(dpReply);
                            String url =nodeList.get(i).url()+"clear-stat";
                            WebTarget target = ClientBuilder.newClient().target(url);
                            Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                            Response response = builder.get();
                            //LOGGER.info(response.getStatus()+"");
                            response.close();
                        }
                        System.out.println("cleared");
                        break;
                    case 9:
                        //query_position++;
                        if (query_position < queryList.size()) {
                            String query_search = queryList.get(query_position++);
                            reply = "0000 QUERY " + nodeList.size();


                            String url =nodeList.get(node_position).url()+"query";
                            WebTarget target = ClientBuilder.newClient().target(url);
                            Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                            Response response = builder.post(Entity.json(nodeList.size()));
                            //LOGGER.info(response.getStatus()+"");
                            response.close();
                            System.out.println("CURRENT QUERY POSITION: " + (query_position) + " query: " + queryList.get(query_position - 1));
                        } else {
                            System.out.println("reached to the limit");
                        }
                        break;
                    case 3:
                        System.out.println("Current Node " + node_position);
                        System.out.println("Enter new node position");
                        Scanner scanner = new Scanner(System.in);
                        try {
                            node_position = Integer.parseInt(scan.nextLine().trim());
                            if (node_position > nodeList.size()) {
                                System.out.println("node is not exist");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input for node position");
                        }

                        query_position = 0;
                        System.out.println("Current node updated: " + node_position);
                        break;
                    case 4:
                        reply = "0000 STAT ";
                        String s = null;
                        for (int i = 0; i < len; i++) {
//                            DatagramPacket statReply = new DatagramPacket(reply.getBytes(), reply.getBytes().length, InetAddress.getByName(nodeList.get(i).getIp()), nodeList.get(i).getPort());
//                            socket.send(statReply);
//
//                            byte[] buffer = new byte[65536];
//                            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
//                            socket.receive(incoming);

                            String url =nodeList.get(i).url()+"get-stats";
                            WebTarget target = ClientBuilder.newClient().target(url);
                            Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                            Response response = builder.post(Entity.json(nodeList.size()));
                            s =response.getEntity().toString();
                            //LOGGER.info(response.getStatus()+"");
                            response.close();

//                            byte[] data = incoming.getData();
//                            s = new String(data, 0, incoming.getLength());
                            System.out.println(s);

                            StringTokenizer tokenizer = new StringTokenizer(s, " ");
                            int count = Integer.parseInt(tokenizer.nextToken());
                            String command = tokenizer.nextToken();

                            Stat stat = new Stat(tokenizer.nextToken());
                            hops += stat.getHops();
                            latencies += stat.getLatencies();
                            received_stat.add(stat.getReceivedMessages());
                            sent_stat.add(stat.getSentMessages());
                            answered_stat.add(stat.getAnsweredMessages());
                            node_degree.add(stat.getNodeDegree());

                            if (stat.getLatencyMax() > 0) {
                                if (hop_min > stat.getHopsMin()) {
                                    hop_min = stat.getHopsMin();
                                }
                                if (hop_max < stat.getHopsMax()) {
                                    hop_max = stat.getHopsMax();
                                }
                                if (latency_min > stat.getLatencyMin()) {
                                    latency_min = stat.getLatencyMin();
                                }
                                if (hop_max < stat.getLatencyMax()) {
                                    latency_max = stat.getLatencyMax();
                                }
                                statList.add(stat);

                            }
                            System.out.println(stat.toString());

                        }
                        max_rec = Collections.max(received_stat);
                        min_rec = Collections.min(received_stat);

                        max_ans = Collections.max(answered_stat);
                        min_ans = Collections.min(answered_stat);

                        max_sent = Collections.max(sent_stat);
                        min_sent = Collections.min(sent_stat);

                        max_ndgree = Collections.max(node_degree);
                        min_ndgree = Collections.min(node_degree);

                        avg_rec = calculateAverage(received_stat);
                        avg_sent = calculateAverage(sent_stat);
                        avg_ans = calculateAverage(answered_stat);
                        avg_ndgree = calculateAverage(node_degree);

                        sd_rec = getSD(received_stat.toArray(), calculateAverage(received_stat));
                        sd_ans = getSD(answered_stat.toArray(), calculateAverage(answered_stat));
                        sd_sent = getSD(sent_stat.toArray(), calculateAverage(sent_stat));
                        sd_ndgree = getSD(node_degree.toArray(), avg_ndgree);

                        CombinedStdDevGeneration stdgen = new CombinedStdDevGeneration(statList);
                        hop_average = stdgen.getSampleMeanOfHops();
                        hop_sd = stdgen.calculateStdDevOfHops();
                        latency_average = stdgen.getSampleMeanOfLatencies();
                        latency_sd = stdgen.calculateStdDevOfLatencies();

                        System.out.println("hop_average" + hop_average);
                        System.out.println("hop sd" + hop_sd);
                        System.out.println("latency average" + latency_average);
                        System.out.println("latancy std" + latency_sd);

                        break;
                    case 5:
                        File file = new File("stat-summary" + fileNumber + ".csv");

                        FileWriter fw = new FileWriter(file);
                        fw.write("received,answered,sent,node degree,latency,hops\n");
                        fw.write(min_rec + "," + min_ans + "," + min_sent + "," + min_ndgree + "," + latency_min + "," + hop_min + "\n");
                        fw.write(max_rec + "," + max_ans + "," + max_sent + "," + max_ndgree + "," + latency_max + "," + hop_max + "\n");
                        fw.write(avg_rec + "," + avg_ans + "," + avg_sent + "," + avg_ndgree + "," + latency_average + "," + hop_average + "\n");
                        fw.write(sd_rec + "," + sd_ans + "," + sd_sent + "," + sd_ndgree + "," + latency_sd + "," + hop_sd);
                        fw.flush();
                        fw.close();

                        file = new File("stat-hops" + fileNumber + ".csv");
                        fw = new FileWriter(file);
                        fw.write(hops.replace(",,", ",").replace(",", "\n"));
                        fw.flush();
                        fw.close();

                        file = new File("stat-latency" + fileNumber + ".csv");
                        fw = new FileWriter(file);
                        fw.write(latencies.replace(",,", ",").replace(",", "\n"));
                        fw.flush();
                        fw.close();


                        file = new File("stats-others" + fileNumber + ".csv");
                        fw = new FileWriter(file);
                        String fout = "sent,received,answered,node degree\n";
                        for (int y = 0; y < received_stat.size(); y++) {
                            fout += sent_stat.get(y) + "," + received_stat.get(y) + "," + answered_stat.get(y) + "," + node_degree.get(y) + "\n";
                        }
                        fw.write(fout);
                        fw.flush();
                        fw.close();


                        max_rec = 0;
                        max_ans = 0;
                        max_sent = 0;
                        max_ndgree = 0;

                        min_rec = 0;
                        min_ans = 0;
                        min_sent = 0;
                        min_ndgree = 0;

                        avg_rec = 0;
                        avg_sent = 0;
                        avg_ans = 0;
                        avg_ndgree = 0;

                        sd_rec = 0;
                        sd_ans = 0;
                        sd_sent = 0;
                        sd_ndgree = 0;

                        hop_min = 9999999;
                        hop_max = 0;
                        hop_sd = 0;
                        hop_average = 0;
                        latency_min = 9999999;
                        latency_max = 0;
                        latency_sd = 0;
                        latency_average = 0;
                        hops = "";
                        latencies = "";


                        received_stat = new ArrayList<>();
                        answered_stat = new ArrayList<>();
                        sent_stat = new ArrayList<>();
                        node_degree = new ArrayList<>();

                        fileNumber++;
                        break;
                    case 6:
                        System.out.println("Enter leaving node position");
                        int leaving = 0;
                        try {
                            leaving = Integer.parseInt(scan.nextLine().trim());
                        } catch (NumberFormatException e) {
                            leaving = -1;
                        }
                        System.out.println("y/n??");
                        answer = (scan.nextLine().trim());
                        if (!answer.equals("y")) {
                            System.out.println("Not left");
                            break;
                        }
                        try {
                            System.out.println("removed node" + nodeList.remove(leaving));
                            System.out.println("Successfullly removed node " + leaving);
                        } catch (Exception e) {
                            System.out.println("Error in removing node");
                        }
                        System.out.println(nodeList);
                        break;
                    case 7:
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid input");
                        break;
                }
            }
//            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static double getSD(Object[] latency, double mean) {
        double variance = 0, sd = 0;
        double[] temp = new double[latency.length];
        for (int i = 0; i < latency.length; i++) {
            temp[i] = (double) (Integer) latency[i] - mean;
            temp[i] = Math.pow(temp[i], 2.0); //to get the (x-average)……2
            variance += temp[i];
        }
        variance = variance / (latency.length - 1); // sample variance
        sd = Math.sqrt(variance);
        return sd;
    }

    private static double calculateAverage(List<Integer> marks) {
        Integer sum = 0;
        if (!marks.isEmpty()) {
            for (Integer mark : marks) {
                sum += mark;
            }
            return sum.doubleValue() / marks.size();
        }
        return sum;
    }
}
