package com.helospark.financialdata.management.watchlist.repository;

import java.nio.ByteBuffer;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Watchlist")
public class Watchlist {
    private String email;
    private ByteBuffer watchlistRaw;

    @DynamoDBHashKey
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
