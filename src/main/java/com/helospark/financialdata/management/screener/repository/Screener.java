package com.helospark.financialdata.management.screener.repository;

import java.nio.ByteBuffer;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Screener")
public class Screener {
    private String email;
    private ByteBuffer screenerRaw;

    @DynamoDBHashKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String key) {
        this.email = key;
    }

    public ByteBuffer getScreenerRaw() {
        return screenerRaw;
    }

    public void setScreenerRaw(ByteBuffer watchlistRaw) {
        this.screenerRaw = watchlistRaw;
    }

}
