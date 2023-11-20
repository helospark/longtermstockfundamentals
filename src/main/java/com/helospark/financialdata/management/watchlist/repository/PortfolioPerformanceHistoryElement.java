package com.helospark.financialdata.management.watchlist.repository;

import java.time.LocalDate;
import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.helospark.financialdata.management.helper.LocalDateConverter;

public class PortfolioPerformanceHistoryElement {
    private String email;
    private LocalDate date;

    private double total;
    private double eps;
    private double fcf;
    private double totalEquity;

    private List<SimpleHolding> holdings;

    @DynamoDBHashKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String key) {
        this.email = key;
    }

    @DynamoDBTypeConverted(converter = LocalDateConverter.class)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getEps() {
        return eps;
    }

    public void setEps(double eps) {
        this.eps = eps;
    }

    public double getFcf() {
        return fcf;
    }

    public void setFcf(double fcfPerShare) {
        this.fcf = fcfPerShare;
    }

    public double getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(double totalEquity) {
        this.totalEquity = totalEquity;
    }

    public List<SimpleHolding> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<SimpleHolding> holdings) {
        this.holdings = holdings;
    }

}
