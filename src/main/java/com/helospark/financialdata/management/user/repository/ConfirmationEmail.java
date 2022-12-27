package com.helospark.financialdata.management.user.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "ConfirmationEmail")
public class ConfirmationEmail {
    private String confirmationId;
    private String email;
    private long expiration;

    @DynamoDBHashKey
    public String getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(String key) {
        this.confirmationId = key;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expire) {
        this.expiration = expire;
    }

}
