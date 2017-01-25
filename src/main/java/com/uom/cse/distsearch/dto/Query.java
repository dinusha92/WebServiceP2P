package com.uom.cse.distsearch.dto;

import com.uom.cse.distsearch.app.Node;
import java.io.Serializable;


public class Query implements Serializable {
    
    private int hops;
    private NodeInfo sender;
    private NodeInfo origin;
    private String query;
    private long timestamp;


    public int getHops() {
        return hops;
    }

    public void setHops(int hops) {
        this.hops = hops;
    }

    public NodeInfo getSender() {
        return sender;
    }

    public void setSender(NodeInfo sender) {
        this.sender = sender;
    }
    
        public NodeInfo getOrigin() {
        return origin;
    }

    public void setOrigin(NodeInfo origin) {
        this.origin = origin;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Query qry = (Query) o;

        if (timestamp != qry.timestamp) return false;
        if (origin != null ? !origin.equals(qry.origin) : qry.origin != null) return false;
        return query != null ? query.equals(qry.query) : qry.query == null;

    }

    @Override
    public int hashCode() {
        int result = origin != null ? origin.hashCode() : 0;
        result = 31 * result + (query != null ? query.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
