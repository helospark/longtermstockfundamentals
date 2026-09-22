package com.helospark.financialdata.management.watchlist.repository;

import java.nio.ByteBuffer;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class PortfolioPerformanceHistory {
    private String email;
    private ByteBuffer history;

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String key) {
        this.email = key;
    }

    public ByteBuffer getHistory() {
        return history;
    }

    public void setHistory(ByteBuffer history) {
        this.history = history;
    }

    @Override
    public String toString() {
        return "PortfolioPerformanceHistory [email=" + email + ", history=" + history + "]";
    }

}
