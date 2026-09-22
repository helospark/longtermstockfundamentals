package com.helospark.financialdata.management.user.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class User {
    private String email;
    private String password;
    private boolean activated;
    private String registered;
    private AccountType accountType;
    private boolean cancelling;
    private RegistrationSource registeredWith;
    private boolean hidePrice;

    @DynamoDbPartitionKey
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

    public RegistrationSource getRegisteredWith() {
        return registeredWith;
    }

    public void setRegisteredWith(RegistrationSource registeredWith) {
        this.registeredWith = registeredWith;
    }

    public boolean isHidePrice() {
        return hidePrice;
    }

    public void setHidePrice(boolean hidePrice) {
        this.hidePrice = hidePrice;
    }

    @Override
    public String toString() {
        return "User [email=" + email + ", activated=" + activated + ", registered=" + registered + ", accountType=" + accountType + ", cancelling=" + cancelling + ", registeredWith=" + registeredWith
                + "]";
    }

}