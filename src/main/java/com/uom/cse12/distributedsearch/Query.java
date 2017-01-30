package com.uom.cse12.distributedsearch;

import java.io.Serializable;


public class Query implements Serializable {
    
    private int hops;
    private Node sender;
    private Node origin;
    private String queryText;
    private int hopeLimit;
    private int queryIndex;
    private long timestamp;

    public int getHops() {
        return hops;
    }

    public void setHops(int hops) {
        this.hops = hops;
    }

    public Node getSender() {
        return sender;
    }

    public void setSender(Node sender) {
        this.sender = sender;
    }
    
    public Node getOrigin() {
        return origin;
    }

    public void setOrigin(Node origin) {
        this.origin = origin;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getQuerryHash(){
        return (""+origin.getIp()).replace(".","")+origin.getPort()+""+ queryIndex +queryText;
    }

    @Override
    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Query qry = (Query) o;
//
//        if (origin != null ? !origin.equals(qry.origin) : qry.origin != null) return false;
//        return queryText != null ? queryText.equals(qry.queryText) : qry.queryText == null;
        Query qry = (Query) o;
        return this.getQuerryHash()==qry.getQuerryHash();

    }

//    @Override
//    public int hashCode() {
//        int result = origin != null ? origin.hashCode() : 0;
//        result = 31 * result + (queryText != null ? queryText.hashCode() : 0);
//        return result;
//    }

    public int getHopeLimit() {
        return hopeLimit;
    }

    public void setHopeLimit(int hopeLimit) {
        this.hopeLimit = hopeLimit;
    }

    public int getQueryIndex() {
        return queryIndex;
    }

    public void setQueryIndex(int queryIndex) {
        this.queryIndex = queryIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
