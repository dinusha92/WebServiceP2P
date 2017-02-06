package com.uom.cse12.distributedsearch;

import java.io.Serializable;

/**
 * Created by kasun on 2/4/17.
 */
public class Stat implements Serializable {
    private int receivedMessages;
    private int sentMessages;
    private int answeredMessages;
    private int nodeDegree;
    private int latencyMin=0;
    private int latencyMax=0;
    private double latencySD=0;
    private double latencyAverage=0;
    private int hopsMin=0;
    private int hopsMax=0;
    private double hopsSD=0;
    private double hopsAverage=0;
    private int numberOfHope=0;
    private int numberOfLatencies =0;
    private String sep = "#";

    public Stat(){

    }


    public int getReceivedMessages() {
        return receivedMessages;
    }

    public void setReceivedMessages(int receivedMessages) {
        this.receivedMessages = receivedMessages;
    }

    public int getSentMessages() {
        return sentMessages;
    }

    public void setSentMessages(int sentMessages) {
        this.sentMessages = sentMessages;
    }

    public int getAnsweredMessages() {
        return answeredMessages;
    }

    public void setAnsweredMessages(int answeredMessages) {
        this.answeredMessages = answeredMessages;
    }

    public int getLatencyMin() {
        return latencyMin;
    }

    public void setLatencyMin(int latencyMin) {
        this.latencyMin = latencyMin;
    }

    public int getLatencyMax() {
        return latencyMax;
    }

    public void setLatencyMax(int latencyMax) {
        this.latencyMax = latencyMax;
    }

    public double getLatencySD() {
        return latencySD;
    }

    public void setLatencySD(double latencySD) {
        this.latencySD = latencySD;
    }

    public int getHopsMin() {
        return hopsMin;
    }

    public void setHopsMin(int hopsMin) {
        this.hopsMin = hopsMin;
    }

    public int getHopsMax() {
        return hopsMax;
    }

    public void setHopsMax(int hopsMax) {
        this.hopsMax = hopsMax;
    }

    public double getHopsSD() {
        return hopsSD;
    }

    public void setHopsSD(double hopsSD) {
        this.hopsSD = hopsSD;
    }

    public double getLatencyAverage() {
        return latencyAverage;
    }

    public void setLatencyAverage(double latencyAverage) {
        this.latencyAverage = latencyAverage;
    }

    public double getHopsAverage() {
        return hopsAverage;
    }

    public void setHopsAverage(double hopsAverage) {
        this.hopsAverage = hopsAverage;
    }

    @Override
    public String toString() {
        String out ="";
        out+="Forwarded Messages\t:"+sentMessages+"\n";
        out+="Received Messages\t:"+receivedMessages+"\n";
        out+="Answered Messages\t:"+answeredMessages+"\n";
        out+="Node degree\t:"+nodeDegree+"\n\n";
        out+="Latency Min\t:"+latencyMin+"\n";
        out+="Latency Max\t:"+latencyMax+"\n";
        out+="No.of Latencies\t:"+numberOfLatencies+"\n";
        out+="Latency Average\t:"+latencyAverage+"\n";
        out+="Latency SD\t:"+latencySD+"\n\n";
        out+="Hops Min\t:"+hopsMin+"\n";
        out+="Hops Max\t:"+hopsMax+"\n";
        out+="No. of hops\t:"+numberOfHope+"\n";
        out+="Hops Average\t:"+hopsAverage+"\n";
        out+="Hops SD\t:"+hopsSD+"\n";
        return out;
    }

    public int getNodeDegree() {
        return nodeDegree;
    }

    public void setNodeDegree(int nodeDegree) {
        this.nodeDegree = nodeDegree;
    }

    public int getNumberOfHope() {
        return numberOfHope;
    }

    public void setNumberOfHope(int numberOfHope) {
        this.numberOfHope = numberOfHope;
    }

    public int getNumberOfLatencies() {
        return numberOfLatencies;
    }

    public void setNumberOfLatencies(int numberOfLatencies) {
        this.numberOfLatencies = numberOfLatencies;
    }
}
