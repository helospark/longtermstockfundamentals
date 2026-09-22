package com.helospark.financialdata.management.screener.repository;

import java.nio.ByteBuffer;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class Screener {
    private String email;
    private ByteBuffer screenerRaw;

    @DynamoDbPartitionKey
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
