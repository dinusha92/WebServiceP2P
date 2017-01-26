package com.uom.cse12.distributedsearch;

import java.io.Serializable;


public class Query implements Serializable {
    
    private int hops;
    private Node sender;
    private Node origin;
    private String query;
    private int hopeLimit;


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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Query qry = (Query) o;

        if (origin != null ? !origin.equals(qry.origin) : qry.origin != null) return false;
        return query != null ? query.equals(qry.query) : qry.query == null;
    }

    @Override
    public int hashCode() {
        int result = origin != null ? origin.hashCode() : 0;
        result = 31 * result + (query != null ? query.hashCode() : 0);
        return result;
    }

    public int getHopeLimit() {
        return hopeLimit;
    }

    public void setHopeLimit(int hopeLimit) {
        this.hopeLimit = hopeLimit;
    }
}
