package com.helospark.financialdata.management.watchlist.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

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

}
