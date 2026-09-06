package com.helospark.financialdata.management.watchlist.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "PortfolioTransactionT")
public class PortfolioTransaction {

    private String userEmail;
    private String transactionDateTime;
    private String symbol;
    private Double amountChange;
    private Double sharePrice;
    private Double transactionValue;
    private Double transactionValueUsd;
    private String currency;

    // Mandatory No-Arg Constructor for DynamoDB Data Mapper reflection
    public PortfolioTransaction() {
    }

    public PortfolioTransaction(String userEmail, String transactionDateTime, String symbol, Double amountChange, Double sharePrice, Double transactionValue, Double transactionValueUsd, String currency) {
        this.userEmail = userEmail;
        this.transactionDateTime = transactionDateTime;
        this.symbol = symbol;
        this.amountChange = amountChange;
        this.sharePrice = sharePrice;
        this.transactionValue = transactionValue;
        this.transactionValueUsd = transactionValueUsd;
        this.currency = currency;
    }

    // --- HASH KEY (Partition) ---
    @DynamoDBHashKey(attributeName = "userEmail")
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    @DynamoDBRangeKey(attributeName = "transactionDateTime")
    public String getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(String transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    @DynamoDBAttribute(attributeName = "symbol")
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @DynamoDBAttribute(attributeName = "amountChange")
    public Double getAmountChange() {
        return amountChange;
    }

    public void setAmountChange(Double amountChange) {
        this.amountChange = amountChange;
    }

    @DynamoDBAttribute(attributeName = "transactionValue")
    public Double getTransactionValue() {
        return transactionValue;
    }

    public void setTransactionValue(Double transactionValue) {
        this.transactionValue = transactionValue;
    }

    public Double getTransactionValueUsd() {
        return transactionValueUsd;
    }

    @DynamoDBAttribute(attributeName = "transactionValueUsd")
    public void setTransactionValueUsd(Double transactionValueUsd) {
        this.transactionValueUsd = transactionValueUsd;
    }

    public String getCurrency() {
        return currency;
    }

    @DynamoDBAttribute(attributeName = "currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getSharePrice() {
        return sharePrice;
    }

    @DynamoDBAttribute(attributeName = "sharePrice")
    public void setSharePrice(Double sharePrice) {
        this.sharePrice = sharePrice;
    }

}
