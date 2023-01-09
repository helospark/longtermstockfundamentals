package com.helospark.financialdata.management.watchlist.domain;

import java.util.List;

public class AddToWatchlistRequest {
    public String symbol;
    public Double priceTarget;
    public List<String> tags;
    public String notes;
    public CalculatorParameters calculatorParameters;
}
