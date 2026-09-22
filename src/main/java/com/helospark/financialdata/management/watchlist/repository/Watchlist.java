package com.helospark.financialdata.management.watchlist.repository;

import java.nio.ByteBuffer;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class Watchlist {
    private String email;
    private ByteBuffer watchlistRaw;

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String key) {
        this.email = key;
    }

    public ByteBuffer getWatchlistRaw() {
        return watchlistRaw;
    }

    public void setWatchlistRaw(ByteBuffer watchlistRaw) {
        this.watchlistRaw = watchlistRaw;
    }

}
