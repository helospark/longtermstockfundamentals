package com.helospark.financialdata.management.watchlist.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class SimpleHolding {
    @JsonProperty("t")
    public String ticket;
    @JsonProperty("c")
    public int count;

    public SimpleHolding(String ticket, int count) {
        this.ticket = ticket;
        this.count = count;
    }

    public SimpleHolding() {
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
