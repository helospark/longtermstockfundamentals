package com.helospark.financialdata.management.payment.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.helospark.financialdata.management.user.repository.AccountType;

@DynamoDBTable(tableName = "StripeUserMapping")
public class StripeUserMapping {
    private String stripeCustomerId;
    private String email;
    private AccountType lastRequestedAccountType;
    private String currentSubscriptionId;

    @DynamoDBHashKey
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

    @DynamoDBTypeConvertedEnum
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
