package com.helospark.financialdata.management.watchlist.repository;

import java.nio.ByteBuffer;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "WatchlistExpectationHistory")
public class WatchlistExpectationHistory {
    private String emailSymbol;
    private ByteBuffer watchlistExpectationListRaw;

    @DynamoDBHashKey
    public String getEmailSymbol() {
        return emailSymbol;
    }

    public void setEmailSymbol(String key) {
        this.emailSymbol = key;
    }

    public ByteBuffer getWatchlistExpectationListRaw() {
        return watchlistExpectationListRaw;
    }

    public void setWatchlistExpectationListRaw(ByteBuffer watchlistRaw) {
        this.watchlistExpectationListRaw = watchlistRaw;
    }

}
