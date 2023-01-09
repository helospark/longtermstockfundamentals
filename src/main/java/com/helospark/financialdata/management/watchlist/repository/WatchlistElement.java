package com.helospark.financialdata.management.watchlist.repository;

import java.util.List;

import com.helospark.financialdata.management.watchlist.domain.CalculatorParameters;

public class WatchlistElement {
    public String symbol;
    public List<String> tags = List.of();
    public Double targetPrice;
    public String notes;
    public CalculatorParameters calculatorParameters;
}
