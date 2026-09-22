package com.helospark.financialdata.management.user.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class PersistentSignin {
    private String key;
    private String email;
    private long expiration;

    @DynamoDbPartitionKey
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    @Override
    public String toString() {
        return "PersistentSignin [key=" + key + ", email=" + email + ", expiration=" + expiration + "]";
    }

}
