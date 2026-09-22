package com.helospark.financialdata.management.payment.repository;

import com.helospark.financialdata.management.user.repository.AccountType;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class StripeUserMapping {
    private String stripeCustomerId;
    private String email;
    private AccountType lastRequestedAccountType;
    private String currentSubscriptionId;

    @DynamoDbPartitionKey
    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AccountType getLastRequestedAccountType() {
        return lastRequestedAccountType;
    }

    public void setLastRequestedAccountType(AccountType lastRequestedAccountType) {
        this.lastRequestedAccountType = lastRequestedAccountType;
    }

    public String getCurrentSubscriptionId() {
        return currentSubscriptionId;
    }

    public void setCurrentSubscriptionId(String currentSubscriptionId) {
        this.currentSubscriptionId = currentSubscriptionId;
    }

}
