package com.helospark.financialdata.management.user.repository;

import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "ViewedStocks")
public class ViewedStocks {
    private String email;
    private Set<String> stocks;

    @DynamoDBHashKey
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
