package com.helospark.financialdata.management.watchlist.repository;

import java.nio.ByteBuffer;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "PortfolioPerformanceHistory")
public class PortfolioPerformanceHistory {
    private String email;
    private ByteBuffer history;

    @DynamoDBHashKey
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
