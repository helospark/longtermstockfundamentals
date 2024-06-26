package com.helospark.financialdata.management.user.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;

@DynamoDBTable(tableName = "User")
public class User {
    private String email;
    private String password;
    private boolean activated;
    private String registered;
    private AccountType accountType;
    private boolean cancelling;
    private RegistrationSource registeredWith;

    @DynamoDBHashKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @DynamoDBTypeConvertedEnum
    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getRegistered() {
        return registered;
    }

    public void setRegistered(String registered) {
        this.registered = registered;
    }

    public boolean isCancelling() {
        return cancelling;
    }

    public void setCancelling(boolean cancelling) {
        this.cancelling = cancelling;
    }

    @DynamoDBTypeConvertedEnum
    public RegistrationSource getRegisteredWith() {
        return registeredWith;
    }

    public void setRegisteredWith(RegistrationSource registeredWith) {
        this.registeredWith = registeredWith;
    }

    @Override
    public String toString() {
        return "User [email=" + email + ", activated=" + activated + ", registered=" + registered + ", accountType=" + accountType + ", cancelling=" + cancelling + ", registeredWith=" + registeredWith
                + "]";
    }

}