package com.helospark.financialdata.management.user.repository;

import java.util.Set;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class ViewedStocks {
    private String email;
    private Set<String> stocks;

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String key) {
        this.email = key;
    }

    public Set<String> getStocks() {
        return stocks;
    }

    public void setStocks(Set<String> stocks) {
        this.stocks = stocks;
    }

}
