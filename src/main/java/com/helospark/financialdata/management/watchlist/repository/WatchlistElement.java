package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

import com.helospark.financialdata.management.watchlist.domain.CalculatorParameters;
import com.helospark.financialdata.management.watchlist.domain.Moats;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class WatchlistElement {
    public String email;
    public String symbol;
    public List<String> tags = List.of();
    public Double targetPrice;
    public String notes;
    public int ownedShares = 0;
    public CalculatorParameters calculatorParameters;
    public Moats moats;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("symbol")
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    // --- Standard Attributes ---
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(Double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getOwnedShares() {
        return ownedShares;
    }

    public void setOwnedShares(int ownedShares) {
        this.ownedShares = ownedShares;
    }

    public CalculatorParameters getCalculatorParameters() {
        return calculatorParameters;
    }

    public void setCalculatorParameters(CalculatorParameters calculatorParameters) {
        this.calculatorParameters = calculatorParameters;
    }

    public Moats getMoats() {
        return moats;
    }

    public void setMoats(Moats moats) {
        this.moats = moats;
    }
}
