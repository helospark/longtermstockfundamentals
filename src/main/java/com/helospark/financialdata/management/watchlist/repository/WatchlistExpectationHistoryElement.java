package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class WatchlistExpectationHistoryElement {
    public String emailSymbolKey;
    public String symbol;
    public String saveDate;
    public String type;
    public double value;
    public double multiple;
    public List<String> dates;
    public List<Double> revenue;
    public List<Double> eps;
    public List<Double> margin;
    public List<Double> shareCount;
    public List<Double> peRatios;

    @DynamoDbPartitionKey
    public String getEmailSymbolKey() {
        return emailSymbolKey;
    }

    public void setEmailSymbolKey(String emailSymbolKey) {
        this.emailSymbolKey = emailSymbolKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @DynamoDbSortKey
    public String getSaveDate() {
        return saveDate;
    }

    public void setSaveDate(String saveDate) {
        this.saveDate = saveDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getMultiple() {
        return multiple;
    }

    public void setMultiple(double multiple) {
        this.multiple = multiple;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public List<Double> getRevenue() {
        return revenue;
    }

    public void setRevenue(List<Double> revenue) {
        this.revenue = revenue;
    }

    public List<Double> getEps() {
        return eps;
    }

    public void setEps(List<Double> eps) {
        this.eps = eps;
    }

    public List<Double> getMargin() {
        return margin;
    }

    public void setMargin(List<Double> margin) {
        this.margin = margin;
    }

    public List<Double> getShareCount() {
        return shareCount;
    }

    public void setShareCount(List<Double> shareCount) {
        this.shareCount = shareCount;
    }

    public List<Double> getPeRatios() {
        return peRatios;
    }

    public void setPeRatios(List<Double> peRatios) {
        this.peRatios = peRatios;
    }

}
