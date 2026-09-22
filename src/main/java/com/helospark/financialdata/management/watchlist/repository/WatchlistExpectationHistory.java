package com.helospark.financialdata.management.watchlist.repository;

import java.nio.ByteBuffer;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class WatchlistExpectationHistory {
    private String emailSymbol;
    private ByteBuffer watchlistExpectationListRaw;

    @DynamoDbPartitionKey
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
